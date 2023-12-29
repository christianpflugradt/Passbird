package de.pflugradts.passbird.domain.service.nest

import de.pflugradts.kotlinextensions.Option
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.shell.Shell
import java.util.stream.Stream

interface NestService {
    fun populate(nestShells: List<Shell>)
    fun place(nestShell: Shell, nestSlot: NestSlot)
    fun discardNestAt(nestSlot: NestSlot)
    fun atNestSlot(nestSlot: NestSlot): Option<Nest>
    fun all(includeDefault: Boolean = false): Stream<Option<Nest>>
    fun currentNest(): Nest
    fun moveToNestAt(nestSlot: NestSlot)
}
