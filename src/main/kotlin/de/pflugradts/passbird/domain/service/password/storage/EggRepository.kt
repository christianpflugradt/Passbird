package de.pflugradts.passbird.domain.service.password.storage

import de.pflugradts.passbird.domain.model.ddd.Repository
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.shell.Shell
import java.util.Optional
import java.util.stream.Stream

interface EggRepository : Repository {
    fun sync()
    fun find(eggIdShell: Shell, nestSlot: NestSlot): Optional<Egg>
    fun find(eggIdShell: Shell): Optional<Egg>
    fun add(egg: Egg)
    fun delete(egg: Egg)
    fun findAll(): Stream<Egg>
}
