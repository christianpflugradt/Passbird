package de.pflugradts.passbird.domain.service.password.tree

import de.pflugradts.passbird.domain.model.ddd.Repository
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.EggId
import de.pflugradts.passbird.domain.model.slot.Slot
import java.util.stream.Stream

interface EggRepository : Repository {
    fun sync()
    fun add(egg: Egg)
    fun delete(egg: Egg)
    fun findAll(slot: Slot): Stream<Egg>
    fun findAll(): Stream<Egg>
    fun memory(): MemoryList
    fun updateMemory(mostRecentEggId: EggId)
}
