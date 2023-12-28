package de.pflugradts.passbird.domain.service

import com.google.inject.Singleton
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.DEFAULT
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.createNest
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.CAPACITY
import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.FIRST_SLOT
import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.LAST_SLOT
import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.nestSlotAt
import de.pflugradts.passbird.domain.model.shell.Shell
import java.util.Collections
import java.util.Optional

@Singleton
class NestingGroundService : NestService {

    private val nests = Collections.nCopies(CAPACITY, EMPTY_NEST).toMutableList()
    private var currentNest = NestSlot.DEFAULT

    override fun populate(nestShells: List<Shell>) {
        if (nestShells.size == CAPACITY) {
            (FIRST_SLOT..LAST_SLOT).forEach {
                if (nestShells[it - 1].isNotEmpty) {
                    nests[it - 1] = createNest(nestShells[it - 1], nestSlotAt(it)).optional()
                }
            }
        }
    }

    override fun place(nestShell: Shell, nestSlot: NestSlot) { nests[nestSlot.nestIndex()] = createNest(nestShell, nestSlot).optional() }
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
