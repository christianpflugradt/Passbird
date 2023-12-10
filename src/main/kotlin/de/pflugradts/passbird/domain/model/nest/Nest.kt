package de.pflugradts.passbird.domain.model.nest

import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf

class Nest private constructor(val bytes: Bytes, val slot: Slot) {
    override fun toString() = "Nest(slot=$slot)"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Nest
        return slot == other.slot
    }
    override fun hashCode() = slot.hashCode()
    companion object {
        val DEFAULT = Nest(bytesOf("Default"), Slot.DEFAULT)
        fun createNest(bytes: Bytes, slot: Slot) = Nest(bytes.copy(), slot)
    }
}
