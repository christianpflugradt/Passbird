package de.pflugradts.passbird.domain.service

import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.shell.Shell
import java.util.Optional
import java.util.stream.Stream

interface NestService {
    fun populate(nestShells: List<Shell>)
    fun place(nestShell: Shell, nestSlot: NestSlot)
    fun atNestSlot(nestSlot: NestSlot): Optional<Nest>
    fun all(includeDefault: Boolean = false): Stream<Optional<Nest>>
    fun currentNest(): Nest
    fun moveToNestAt(nestSlot: NestSlot)
}
