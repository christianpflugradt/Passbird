package de.pflugradts.passbird.domain.service.password.tree

import de.pflugradts.kotlinextensions.MutableOption
import de.pflugradts.kotlinextensions.MutableOption.Companion.mutableOptionOf
import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.EggIdFavorites
import de.pflugradts.passbird.domain.model.egg.FavoriteMap
import de.pflugradts.passbird.domain.model.egg.MemoryMap
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.nest.NestService
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.util.function.Predicate

@Singleton
class NestingGround @Inject constructor(
    @param:Named("EggIdMemoryEnabled")
    private val eggIdMemoryEnabled: Boolean,
    private val passwordTreeAdapterPort: PasswordTreeAdapterPort,
    private val nestService: NestService,
    private val eventRegistry: EventRegistry,
) : EggRepository {
    private val lazyFavorites: MutableOption<FavoriteMap> = mutableOptionOf()
    private val lazyMemory: MutableOption<MemoryMap> = mutableOptionOf()
    private val lazyEggs: MutableOption<MutableList<Egg>> = mutableOptionOf()
    private val favorites: FavoriteMap get() = initializeIfEmpty().run { lazyFavorites.get() }
    private val memory: MemoryMap get() = initializeIfEmpty().run { lazyMemory.get() }
    private val eggs: MutableList<Egg> get() = initializeIfEmpty().run { lazyEggs.get() }
    private val currentNestSlot get() = nestService.currentNest().slot

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
        }
    }

    override fun add(egg: Egg) {
        eventRegistry.register(egg)
        eggs.add(egg)
        updateMemory(egg)
    }

    override fun delete(egg: Egg) {
        eggs.remove(egg)
        eventRegistry.deregister(egg)
    }

    override fun sync(): TryResult<Unit> = passwordTreeAdapterPort.sync(EggStreamSupplier({ eggs.stream() }, memory, favorites))
    override fun findAll(slot: Slot) = createEggStreamSupplier(slot).get()
    override fun findAll() = createEggStreamSupplier(inNest(currentNestSlot)).get()
    private fun createEggStreamSupplier(slot: Slot) = createEggStreamSupplier(inNest(slot))
    private fun createEggStreamSupplier(predicate: Predicate<Egg>) = EggStreamSupplier({ eggs.stream().filter(predicate) })

    override fun favorites() = favorites[currentNestSlot].get().copy()
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
    override fun renameFavorites(nestSlot: Slot, from: EncryptedShell, to: EncryptedShell) {
        favorites[nestSlot].get().rename(from, to)
    }
    override fun updateMemory(mostRecentEgg: Egg, duplicate: EncryptedShell?) {
        if (eggIdMemoryEnabled) {
            memory[currentNestSlot].get().memorize(mostRecentEgg.viewEggId(), duplicate).also { sync() }
        }
    }
}

private fun inNest(slot: Slot) = Predicate<Egg> { it.associatedNest() == slot }
