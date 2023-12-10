package de.pflugradts.passbird.domain.service

import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.transfer.Bytes
import java.util.Optional
import java.util.stream.Stream

interface NestService {
    fun populate(nestBytes: List<Bytes>)
    fun deploy(nestBytes: Bytes, slot: Slot)
    fun atSlot(slot: Slot): Optional<Nest>
    fun all(includeDefault: Boolean = false): Stream<Optional<Nest>>
    fun getCurrentNest(): Nest
    fun moveToNestAt(slot: Slot)
}
