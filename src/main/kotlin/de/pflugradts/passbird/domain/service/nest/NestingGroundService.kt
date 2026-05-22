package de.pflugradts.passbird.domain.service.nest

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.kotlinextensions.MutableOption
import de.pflugradts.kotlinextensions.MutableOption.Companion.mutableOptionOf
import de.pflugradts.kotlinextensions.MutableOption.Companion.optionOf
import de.pflugradts.kotlinextensions.Option
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.createNest
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.CAPACITY
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.tree.EggRepository
import java.util.function.Supplier

@Singleton
class NestingGroundService @Inject constructor(
    private val eggRepository: EggRepository,
    private val eventRegistry: EventRegistry,
) : NestService {

    private val lazyNests = mutableListOf<MutableOption<Nest>>()
    private val nests: MutableList<MutableOption<Nest>> get() {
        if (lazyNests.isEmpty()) {
            repeat(CAPACITY) { lazyNests.add(EMPTY_NEST_SUPPLIER.get()) }
            eggRepository.sync()
        }
        return lazyNests
    }
    private var currentNest = Slot.DEFAULT

    override fun populate(nestShells: List<Shell>) {
        if (nestShells.size == Slot.CAPACITY) {
            nestShells.forEachIndexed { index, shell ->
                if (shell.isNotEmpty) {
                    place(shell, Slot.slotAt(index + 1), publish = false)
                }
            }
        }
    }

    override fun place(nestShell: Shell, slot: Slot) = place(nestShell, slot, true)
    fun place(nestShell: Shell, slot: Slot, publish: Boolean) {
        createNest(nestShell, slot).let {
            nests[slot.nestIndex()].set(it)
            eventRegistry.register(it)
            if (publish) eventRegistry.processEvents() else it.clearDomainEvents()
        }
    }
    override fun discardNestAt(slot: Slot) {
        atNestSlot(slot).ifPresent { it.discard() }
        nests[slot.nestIndex()] = EMPTY_NEST_SUPPLIER.get()
        eventRegistry.processEvents()
    }
    override fun atNestSlot(slot: Slot): Option<Nest> = if (slot === Slot.DEFAULT) Nest.DEFAULT.option() else nests[slot.nestIndex()]
    override fun all(includeDefault: Boolean) = nests.let { if (includeDefault) Nest.DEFAULT.asOptionInList() + it else it }.stream()
    override fun currentNest(): Nest = atNestSlot(currentNest).orElse(Nest.DEFAULT)
    override fun moveToNestAt(slot: Slot) {
        if (atNestSlot(slot).isPresent) currentNest = slot
    }

    companion object {
        private val EMPTY_NEST_SUPPLIER = Supplier { mutableOptionOf<Nest>() }
    }
}

private fun Nest.asOptionInList() = listOf(optionOf(this))
private fun Nest.option() = optionOf(this)
private fun Slot.nestIndex() = index() - 1
