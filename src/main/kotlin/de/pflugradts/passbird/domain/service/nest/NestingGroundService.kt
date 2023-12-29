package de.pflugradts.passbird.domain.service.nest

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.storage.EggRepository
import java.util.Collections
import java.util.Optional

@Singleton
class NestingGroundService @Inject constructor(
    @Inject private val eggRepository: EggRepository,
    @Inject private val eventRegistry: EventRegistry,
) : NestService {

    private val lazyNests = mutableListOf<Optional<Nest>>()
    private val nests: MutableList<Optional<Nest>> get() {
        if (lazyNests.isEmpty()) {
            lazyNests.addAll(Collections.nCopies(NestSlot.CAPACITY, EMPTY_NEST).toMutableList())
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
        Nest.createNest(nestShell, nestSlot).let {
            nests[nestSlot.nestIndex()] = it.optional()
            eventRegistry.register(it)
            if (publish) eventRegistry.processEvents() else it.clearDomainEvents()
        }
    }
    override fun discardNestAt(nestSlot: NestSlot) {
        atNestSlot(nestSlot).ifPresent { it.discard() }
        nests[nestSlot.nestIndex()] = EMPTY_NEST
        eventRegistry.processEvents()
    }
    override fun atNestSlot(nestSlot: NestSlot): Optional<Nest> =
        if (nestSlot === NestSlot.DEFAULT) Nest.DEFAULT.optional() else nests[nestSlot.nestIndex()]
    override fun all(includeDefault: Boolean) = nests.let { if (includeDefault) Nest.DEFAULT.asOptionalInList() + it else it }.stream()
    override fun currentNest(): Nest = atNestSlot(currentNest).orElse(Nest.DEFAULT)
    override fun moveToNestAt(nestSlot: NestSlot) { if (atNestSlot(nestSlot).isPresent) { currentNest = nestSlot } }

    companion object { private val EMPTY_NEST = Optional.empty<Nest>() }
}

private fun Nest.asOptionalInList() = listOf(Optional.of(this))
private fun Nest.optional() = Optional.of(this)
private fun NestSlot.nestIndex() = index() - 1
