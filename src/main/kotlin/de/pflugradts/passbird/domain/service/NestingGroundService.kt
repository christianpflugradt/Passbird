package de.pflugradts.passbird.domain.service

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.DEFAULT
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.createNest
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.CAPACITY
import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.nestSlotAt
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import java.util.Collections
import java.util.Optional

@Singleton
class NestingGroundService @Inject constructor(@Inject private val eventRegistry: EventRegistry) : NestService {

    private val nests = Collections.nCopies(CAPACITY, EMPTY_NEST).toMutableList()
    private var currentNest = NestSlot.DEFAULT

    override fun populate(nestShells: List<Shell>) {
        if (nestShells.size == CAPACITY) {
            nestShells.forEachIndexed { index, shell ->
                if (shell.isNotEmpty) {
                    place(shell, nestSlotAt(index + 1), publish = false)
                }
            }
        }
    }

    override fun place(nestShell: Shell, nestSlot: NestSlot) = place(nestShell, nestSlot, true)
    fun place(nestShell: Shell, nestSlot: NestSlot, publish: Boolean) {
        createNest(nestShell, nestSlot).let {
            nests[nestSlot.nestIndex()] = it.optional()
            eventRegistry.register(it)
            if (publish) eventRegistry.processEvents() else it.clearDomainEvents()
        }
    }
    override fun discard(nestSlot: NestSlot) { nests[nestSlot.nestIndex()] = EMPTY_NEST }
    override fun atNestSlot(nestSlot: NestSlot): Optional<Nest> =
        if (nestSlot === NestSlot.DEFAULT) DEFAULT.optional() else nests[nestSlot.nestIndex()]
    override fun all(includeDefault: Boolean) = nests.let { if (includeDefault) DEFAULT.asOptionalInList() + it else it }.stream()
    override fun currentNest(): Nest = atNestSlot(currentNest).orElse(DEFAULT)
    override fun moveToNestAt(nestSlot: NestSlot) { if (atNestSlot(nestSlot).isPresent) { currentNest = nestSlot } }

    companion object { private val EMPTY_NEST = Optional.empty<Nest>() }
}

private fun Nest.asOptionalInList() = listOf(Optional.of(this))
private fun Nest.optional() = Optional.of(this)
private fun NestSlot.nestIndex() = index() - 1
