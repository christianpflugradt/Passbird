package de.pflugradts.passbird.domain.model.nest

import de.pflugradts.passbird.domain.model.ddd.AggregateRoot
import de.pflugradts.passbird.domain.model.event.NestCreated
import de.pflugradts.passbird.domain.model.event.NestDiscarded
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot

class Nest private constructor(private val shell: Shell, val slot: Slot) : AggregateRoot() {
    init {
        registerDomainEvent(NestCreated(this))
    }
    fun viewNestId() = shell.copy()

    fun discard() {
        registerDomainEvent(NestDiscarded(this))
    }

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
