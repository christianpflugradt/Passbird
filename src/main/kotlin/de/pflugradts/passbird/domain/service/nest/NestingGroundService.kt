package de.pflugradts.passbird.domain.service.nest
import de.pflugradts.kotlinextensions.MutableOption
import de.pflugradts.kotlinextensions.MutableOption.Companion.mutableOptionOf
import de.pflugradts.kotlinextensions.MutableOption.Companion.optionOf
import de.pflugradts.kotlinextensions.Option
import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.kotlinextensions.TryResult.Companion.success
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.createNest
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.CAPACITY
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.FIRST_SLOT
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.LAST_SLOT
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.slotAt
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.tree.PasswordTreeAdapterPort
import de.pflugradts.passbird.domain.service.password.tree.PasswordTreeSyncService
import java.util.function.Supplier
class NestingGroundService constructor(
    private val passwordTreeAdapterPort: PasswordTreeAdapterPort,
    private val passwordTreeSyncService: PasswordTreeSyncService,
    private val eventRegistry: EventRegistry,
) : NestService, NestStateView {
    private val lazyNests = mutableListOf<MutableOption<Nest>>()
    private val nests: MutableList<MutableOption<Nest>> get() = lazyNests.also { restoreIfNeeded() }
    private var currentNest = Slot.DEFAULT
    override fun populate(nestShells: List<Shell>) {
        initializeSlotsIfNeeded()
        restoreFromShells(nestShells)
    }
    override fun place(nestShell: Shell, slot: Slot) = place(nestShell, slot, true)
    fun place(nestShell: Shell, slot: Slot, publish: Boolean): TryResult<Unit> {
        restoreIfNeeded()
        val snapshot = nestSnapshot()
        val currentNestSnapshot = currentNest
        createNest(nestShell, slot).let {
            lazyNests[slot.nestIndex()].set(it)
            eventRegistry.register(it)
            if (publish) {
                return passwordTreeSyncService.sync()
                    .onSuccess { eventRegistry.processEvents() }
                    .onFailure { restore(snapshot, currentNestSnapshot) }
            }
            it.clearDomainEvents()
            return success(Unit)
        }
    }
    override fun discardNestAt(slot: Slot): TryResult<Unit> {
        val snapshot = nestSnapshot()
        val currentNestSnapshot = currentNest
        atNestSlot(slot).ifPresent {
            it.discard()
            it.getDomainEvents().forEach(eventRegistry::register)
            it.clearDomainEvents()
            eventRegistry.deregister(it)
        }
        lazyNests[slot.nestIndex()] = EMPTY_NEST_SUPPLIER.get()
        return passwordTreeSyncService.sync()
            .onSuccess { eventRegistry.processEvents() }
            .onFailure { restore(snapshot, currentNestSnapshot) }
    }
    override fun atNestSlot(slot: Slot): Option<Nest> = if (slot === Slot.DEFAULT) Nest.DEFAULT.option() else nests[slot.nestIndex()]
    override fun all(includeDefault: Boolean) = nests.let { if (includeDefault) Nest.DEFAULT.asOptionInList() + it else it }.stream()
    override fun currentNest(): Nest = atNestSlot(currentNest).orElse(Nest.DEFAULT)
    override fun moveToNestAt(slot: Slot) {
        if (atNestSlot(slot).isPresent) currentNest = slot
    }
    override fun currentNestSlot() = currentNest().slot
    override fun snapshot() = nestSnapshot()
    private fun nestSnapshot() = (FIRST_SLOT..LAST_SLOT).map { slot ->
        atNestSlot(slotAt(slot)).map { it.viewNestId() }.orElse(emptyShell())
    }
    private fun initializeSlotsIfNeeded() {
        if (lazyNests.isEmpty()) {
            repeat(CAPACITY) { lazyNests.add(EMPTY_NEST_SUPPLIER.get()) }
        }
    }
    private fun restoreIfNeeded() {
        if (lazyNests.isEmpty()) {
            initializeSlotsIfNeeded()
            restoreFromShells(passwordTreeAdapterPort.restore().nests())
        }
    }
    private fun restore(nestShells: List<Shell>, currentNestSnapshot: Slot) {
        lazyNests.filter { it.isPresent }.forEach { eventRegistry.deregister(it.get()) }
        eventRegistry.clearEvents()
        lazyNests.clear()
        currentNest = currentNestSnapshot
        initializeSlotsIfNeeded()
        restoreFromShells(nestShells)
    }
    private fun restoreFromShells(nestShells: List<Shell>) {
        if (nestShells.size == Slot.CAPACITY) {
            nestShells.forEachIndexed { index, shell ->
                if (shell.isNotEmpty) {
                    place(shell, Slot.slotAt(index + 1), publish = false)
                }
            }
        }
        if (currentNest != Slot.DEFAULT && atNestSlot(currentNest).isEmpty) {
            currentNest = Slot.DEFAULT
        }
    }
    companion object {
        private val EMPTY_NEST_SUPPLIER = Supplier { mutableOptionOf<Nest>() }
    }
}
private fun Nest.asOptionInList() = listOf(optionOf(this))
private fun Nest.option() = optionOf(this)
private fun Slot.nestIndex() = index() - 1
