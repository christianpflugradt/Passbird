package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.ddd.AggregateRoot
import de.pflugradts.passbird.domain.model.egg.EggId.Companion.createEggId
import de.pflugradts.passbird.domain.model.egg.Password.Companion.createPassword
import de.pflugradts.passbird.domain.model.event.EggCreated
import de.pflugradts.passbird.domain.model.event.EggDiscarded
import de.pflugradts.passbird.domain.model.event.EggMoved
import de.pflugradts.passbird.domain.model.event.EggRenamed
import de.pflugradts.passbird.domain.model.event.EggUpdated
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.shell.Shell

class Egg private constructor(
    private var nestSlot: NestSlot,
    private val eggId: EggId,
    private val password: Password,
) : AggregateRoot() {

    init { registerDomainEvent(EggCreated(this)) }
    fun associatedNest() = nestSlot
    fun viewEggId() = eggId.view()
    fun viewPassword() = password.view()

    fun rename(eggIdShell: Shell) {
        eggId.rename(eggIdShell)
        registerDomainEvent(EggRenamed(this))
    }

    fun updatePassword(shell: Shell) {
        password.update(shell)
        registerDomainEvent(EggUpdated(this))
    }

    fun moveToNestAt(nestSlot: NestSlot) {
        this.nestSlot = nestSlot
        registerDomainEvent(EggMoved(this))
    }

    fun discard() {
        password.discard()
        registerDomainEvent(EggDiscarded(this))
    }

    override fun equals(other: Any?) =
        (other as? Egg)?.let { it.viewEggId() == viewEggId() && it.associatedNest() == nestSlot } ?: false
    override fun hashCode() = nestSlot.hashCode() + 31 * eggId.hashCode()

    companion object {
        fun createEgg(nestSlot: NestSlot, eggIdShell: Shell, passwordShell: Shell) =
            Egg(nestSlot, createEggId(eggIdShell), createPassword(passwordShell))
    }
}
