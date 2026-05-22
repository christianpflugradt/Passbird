package de.pflugradts.passbird.domain.service.password.tree

import de.pflugradts.passbird.domain.model.ddd.Repository
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.EggIdMemory
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.slot.Slot
import java.util.stream.Stream

interface EggRepository : Repository {
    fun sync()
    fun add(egg: Egg)
    fun delete(egg: Egg)
    fun findAll(slot: Slot): Stream<Egg>
    fun findAll(): Stream<Egg>
    fun memory(): EggIdMemory
    fun updateMemory(mostRecentEgg: Egg, duplicate: EncryptedShell? = null)
}
