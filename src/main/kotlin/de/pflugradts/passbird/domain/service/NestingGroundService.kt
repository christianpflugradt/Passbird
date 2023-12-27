package de.pflugradts.passbird.domain.service

import com.google.inject.Singleton
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.DEFAULT
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.createNest
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.CAPACITY
import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.FIRST_SLOT
import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.LAST_SLOT
import de.pflugradts.passbird.domain.model.shell.Shell
import java.util.Collections
import java.util.Optional

@Singleton
class NestingGroundService : NestService {

    private val nests = Collections.nCopies(CAPACITY, Optional.empty<Nest>()).toMutableList()
    private var currentNest = NestSlot.DEFAULT

    override fun populate(nestShells: List<Shell>) {
        if (nestShells.size == CAPACITY) {
            (FIRST_SLOT..LAST_SLOT).forEach {
                if (nestShells[it - 1].isNotEmpty) {
                    nests[it - 1] = Optional.of(createNest(nestShells[it - 1], NestSlot.nestSlotAt(it)))
                }
            }
        }
    }

    override fun place(nestShell: Shell, nestSlot: NestSlot) {
        nests[nestSlot.index() - 1] = Optional.of(createNest(nestShell, nestSlot))
    }
    override fun atNestSlot(nestSlot: NestSlot): Optional<Nest> =
        if (nestSlot === NestSlot.DEFAULT) Optional.of(DEFAULT) else nests[nestSlot.index() - 1]
    override fun all(includeDefault: Boolean) = nests.let { if (includeDefault) DEFAULT.asOptionalInList() + it else it }.stream()
    override fun currentNest(): Nest = atNestSlot(currentNest).orElse(DEFAULT)
    override fun moveToNestAt(nestSlot: NestSlot) {
        if (atNestSlot(nestSlot).isPresent) { currentNest = nestSlot }
    }
}

private fun Nest.asOptionalInList() = listOf(Optional.of(this))
