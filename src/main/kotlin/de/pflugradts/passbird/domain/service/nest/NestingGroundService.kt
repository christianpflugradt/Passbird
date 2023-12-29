package de.pflugradts.passbird.domain.service.nest

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.kotlinextensions.MutableOption
import de.pflugradts.kotlinextensions.MutableOption.Companion.mutableOptionOf
import de.pflugradts.kotlinextensions.MutableOption.Companion.optionOf
import de.pflugradts.kotlinextensions.Option
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.createNest
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.CAPACITY
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.storage.EggRepository
import java.util.function.Supplier

@Singleton
class NestingGroundService @Inject constructor(
    @Inject private val eggRepository: EggRepository,
    @Inject private val eventRegistry: EventRegistry,
) : NestService {

    private val lazyNests = mutableListOf<MutableOption<Nest>>()
    private val nests: MutableList<MutableOption<Nest>> get() {
        if (lazyNests.isEmpty()) {
            repeat(CAPACITY) { lazyNests.add(EMPTY_NEST_SUPPLIER.get()) }
            eggRepository.sync()
        }
        return lazyNests
    }
    private var currentNest = NestSlot.DEFAULT

    override fun populate(nestShells: List<Shell>) {
        if (nestShells.size == NestSlot.CAPACITY) {
            nestShells.forEachIndexed { index, shell ->
                if (shell.isNotEmpty) {
                    place(shell, NestSlot.nestSlotAt(index + 1), publish = false)
                }
            }
        }
    }

    override fun place(nestShell: Shell, nestSlot: NestSlot) = place(nestShell, nestSlot, true)
    fun place(nestShell: Shell, nestSlot: NestSlot, publish: Boolean) {
        createNest(nestShell, nestSlot).let {
            nests[nestSlot.nestIndex()].set(it)
            eventRegistry.register(it)
            if (publish) eventRegistry.processEvents() else it.clearDomainEvents()
        }
    }
    override fun discardNestAt(nestSlot: NestSlot) {
        atNestSlot(nestSlot).ifPresent { it.discard() }
        nests[nestSlot.nestIndex()] = EMPTY_NEST_SUPPLIER.get()
        eventRegistry.processEvents()
    }
    override fun atNestSlot(nestSlot: NestSlot): Option<Nest> =
        if (nestSlot === NestSlot.DEFAULT) Nest.DEFAULT.option() else nests[nestSlot.nestIndex()]
    override fun all(includeDefault: Boolean) = nests.let { if (includeDefault) Nest.DEFAULT.asOptionInList() + it else it }.stream()
    override fun currentNest(): Nest = atNestSlot(currentNest).orElse(Nest.DEFAULT)
    override fun moveToNestAt(nestSlot: NestSlot) { if (atNestSlot(nestSlot).isPresent) { currentNest = nestSlot } }

    companion object { private val EMPTY_NEST_SUPPLIER = Supplier { mutableOptionOf<Nest>() } }
}

private fun Nest.asOptionInList() = listOf(optionOf(this))
private fun Nest.option() = optionOf(this)
private fun NestSlot.nestIndex() = index() - 1
