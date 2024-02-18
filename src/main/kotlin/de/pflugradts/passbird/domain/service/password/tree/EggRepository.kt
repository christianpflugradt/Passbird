package de.pflugradts.passbird.domain.service.password.tree

import de.pflugradts.kotlinextensions.Option
import de.pflugradts.passbird.domain.model.ddd.Repository
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.slot.Slot
import java.util.stream.Stream

interface EggRepository : Repository {
    fun sync()
    fun find(eggIdShell: Shell, slot: Slot): Option<Egg>
    fun find(eggIdShell: Shell): Option<Egg>
    fun add(egg: Egg)
    fun delete(egg: Egg)
    fun findAll(): Stream<Egg>
}
