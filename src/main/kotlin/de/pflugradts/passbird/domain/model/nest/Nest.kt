package de.pflugradts.passbird.domain.model.nest

import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf

class Nest private constructor(val shell: Shell, val nestSlot: NestSlot) {
    override fun toString() = "Nest(nestSlot=$nestSlot)"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Nest
        return nestSlot == other.nestSlot
    }
    override fun hashCode() = nestSlot.hashCode()
    companion object {
        val DEFAULT = Nest(shellOf("Default"), NestSlot.DEFAULT)
        fun createNest(shell: Shell, nestSlot: NestSlot) = Nest(shell.copy(), nestSlot)
    }
}
