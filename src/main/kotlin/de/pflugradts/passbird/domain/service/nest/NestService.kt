package de.pflugradts.passbird.domain.service.nest

import de.pflugradts.kotlinextensions.Option
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.slot.Slot
import java.util.stream.Stream

interface NestService {
    fun populate(nestShells: List<Shell>)
    fun place(nestShell: Shell, slot: Slot)
    fun discardNestAt(slot: Slot)
    fun atNestSlot(slot: Slot): Option<Nest>
    fun all(includeDefault: Boolean = false): Stream<Option<Nest>>
    fun currentNest(): Nest
    fun moveToNestAt(slot: Slot)
}
