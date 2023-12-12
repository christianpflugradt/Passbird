package de.pflugradts.passbird.domain.model.nest

import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf

class Nest private constructor(val shell: Shell, val slot: Slot) {
    override fun toString() = "Nest(slot=$slot)"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Nest
        return slot == other.slot
    }
    override fun hashCode() = slot.hashCode()
    companion object {
        val DEFAULT = Nest(shellOf("Default"), Slot.DEFAULT)
        fun createNest(shell: Shell, slot: Slot) = Nest(shell.copy(), slot)
    }
}
