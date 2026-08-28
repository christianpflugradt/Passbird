package de.pflugradts.passbird.domain.service.password.tree
import de.pflugradts.kotlinextensions.MutableOption
import de.pflugradts.kotlinextensions.MutableOption.Companion.mutableOptionOf
import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.EggIdFavorites
import de.pflugradts.passbird.domain.model.egg.EggIdMemory
import de.pflugradts.passbird.domain.model.egg.FavoriteMap
import de.pflugradts.passbird.domain.model.egg.MemoryMap
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.nest.NestStateMover
import de.pflugradts.passbird.domain.service.nest.NestStateView
import java.util.function.Predicate

class NestingGround constructor(
    private val eggIdMemoryEnabled: Boolean,
    private val passwordTreeAdapterPort: PasswordTreeAdapterPort,
    private val nestStateView: NestStateView,
    private val eventRegistry: EventRegistry,
) : EggRepository, NestStateMover {
    private val lazyFavorites: MutableOption<FavoriteMap> = mutableOptionOf()
    private val lazyMemory: MutableOption<MemoryMap> = mutableOptionOf()
    private val lazyEggs: MutableOption<MutableList<Egg>> = mutableOptionOf()
    private var lastSyncedSnapshot: PasswordTreeState? = null
    private val favorites: FavoriteMap get() = initializeIfEmpty().run { lazyFavorites.get() }
    private val memory: MemoryMap get() = initializeIfEmpty().run { lazyMemory.get() }
    private val eggs: MutableList<Egg> get() = initializeIfEmpty().run { lazyEggs.get() }
    private val currentNestSlot get() = nestStateView.currentNestSlot()
    private fun initializeIfEmpty() {
        if (lazyEggs.isEmpty) {
            val initialState = passwordTreeAdapterPort.restore()
            lazyEggs.set(initialState.get().toList().toMutableList())
            lazyFavorites.set(initialState.favorites())
            lazyMemory.set(initialState.memory())
            lazyEggs.get().forEach {
                it.clearDomainEvents()
                eventRegistry.register(it)
            }
            lastSyncedSnapshot = snapshot()
        }
    }
    override fun add(egg: Egg) {
        eventRegistry.register(egg)
        eggs.add(egg)
        updateMemory(egg, sync = false)
    }
    override fun delete(egg: Egg) {
        eggs.remove(egg)
        eventRegistry.deregister(egg)
    }
    override fun sync(): TryResult<Unit> {
        discardFavoritesForEmptyNests()
        val snapshot = snapshot()
        return passwordTreeAdapterPort.sync(snapshot.toEggStreamSupplier())
            .onSuccess { lastSyncedSnapshot = snapshot }
            .onFailure { restoreLastSyncedSnapshot() }
    }
    override fun findAll(slot: Slot) = createEggStreamSupplier(slot).get()
    override fun findAll() = createEggStreamSupplier(inNest(currentNestSlot)).get()
    override fun findAllIncludingTrashed(slot: Slot) = createEggStreamSupplier(inNest(slot), includeTrashed = true).get()
    override fun findAllIncludingTrashed() = createEggStreamSupplier(inNest(currentNestSlot), includeTrashed = true).get()
    override fun findAllTrashed() = createEggStreamSupplier(Predicate(Egg::isTrashed), includeTrashed = true).get()
    private fun createEggStreamSupplier(slot: Slot) = createEggStreamSupplier(inNest(slot))
    private fun createEggStreamSupplier(predicate: Predicate<Egg>, includeTrashed: Boolean = false) = EggStreamSupplier({
        eggs.stream()
            .filter(predicate)
            .filter { egg -> includeTrashed || !egg.isTrashed() }
    })
    override fun favorites(slot: Slot) = favorites[slot].get().copy()
    override fun favorites() = favorites(currentNestSlot)
    override fun memory() = memory[currentNestSlot].get().copy()
    override fun putFavorite(slot: Slot, encryptedShell: EncryptedShell) {
        favorites[currentNestSlot].get().assign(slot, encryptedShell)
    }
    override fun discardFavorite(slot: Slot) {
        favorites[currentNestSlot].get().discard(slot)
    }
    override fun discardFavorites(nestSlot: Slot, encryptedShell: EncryptedShell) {
        favorites[nestSlot].get().discard(encryptedShell)
    }
    override fun discardFavorites(nestSlot: Slot) {
        favorites[nestSlot].set(EggIdFavorites())
    }
    override fun discardMemory(nestSlot: Slot, encryptedShell: EncryptedShell) {
        memory[nestSlot].get().discard(encryptedShell)
    }
    override fun moveNest(from: Slot, to: Slot) {
        eggs.filter { it.associatedNest() == from }.forEach {
            it.moveToNestAt(to)
            it.clearDomainEvents()
        }
        favorites[to].set(favorites[from].get().copy())
        favorites[from].set(EggIdFavorites())
        memory[to].set(memory[from].get().copy())
        memory[from].set(EggIdMemory())
    }
    override fun renameFavorites(nestSlot: Slot, from: EncryptedShell, to: EncryptedShell) {
        favorites[nestSlot].get().rename(from, to)
    }
    override fun updateMemory(mostRecentEgg: Egg, duplicate: EncryptedShell?, sync: Boolean) {
        if (eggIdMemoryEnabled) {
            memory[currentNestSlot].get().memorize(mostRecentEgg.viewEggId(), duplicate)
            if (sync) sync()
        }
    }
    private fun discardFavoritesForEmptyNests() {
        nestStateView.snapshot().forEachIndexed { index, shell ->
            if (shell.isEmpty) {
                favorites[Slot.slotAt(index + 1)].set(EggIdFavorites())
            }
        }
    }
    private fun snapshot() = PasswordTreeState(
        eggs = eggs.map(Egg::copy),
        memory = memory.copyUsing(EggIdMemory::copy),
        favorites = favorites.copyUsing(EggIdFavorites::copy),
        nests = nestStateView.snapshot(),
    )
    private fun restoreLastSyncedSnapshot() {
        lastSyncedSnapshot?.let { snapshot ->
            eggs.forEach(eventRegistry::deregister)
            eventRegistry.clearEvents()
            lazyEggs.set(snapshot.eggs.map(Egg::copy).toMutableList())
            lazyMemory.set(snapshot.memory.copyUsing(EggIdMemory::copy))
            lazyFavorites.set(snapshot.favorites.copyUsing(EggIdFavorites::copy))
            lazyEggs.get().forEach(eventRegistry::register)
        }
    }
}
private fun inNest(slot: Slot) = Predicate<Egg> { it.associatedNest() == slot }
private data class PasswordTreeState(
    val eggs: List<Egg>,
    val memory: MemoryMap,
    val favorites: FavoriteMap,
    val nests: List<Shell>,
) {
    fun toEggStreamSupplier() = EggStreamSupplier({ eggs.map(Egg::copy).stream() }, memory, favorites, nests)
}
