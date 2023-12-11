package de.pflugradts.passbird.domain.service.password.storage

import de.pflugradts.passbird.domain.model.ddd.Repository
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.transfer.Bytes
import java.util.Optional
import java.util.stream.Stream

interface EggRepository : Repository {
    fun sync()
    fun find(keyBytes: Bytes, nestSlot: Slot): Optional<Egg>
    fun find(keyBytes: Bytes): Optional<Egg>
    fun add(egg: Egg)
    fun delete(egg: Egg)
    fun findAll(): Stream<Egg>
}
