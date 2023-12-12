package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.ddd.AggregateRoot
import de.pflugradts.passbird.domain.model.egg.EggId.Companion.createEggId
import de.pflugradts.passbird.domain.model.egg.Password.Companion.createPassword
import de.pflugradts.passbird.domain.model.event.EggCreated
import de.pflugradts.passbird.domain.model.event.EggDiscarded
import de.pflugradts.passbird.domain.model.event.EggRenamed
import de.pflugradts.passbird.domain.model.event.EggUpdated
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.shell.Shell

/**
 * An Egg represents an [EggName] and an associated [Password] stored in the
 * [PasswordStore][PasswordStoreAdapterPort].
 */
class Egg private constructor(
    private var nestSlot: Slot,
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

    fun moveToNestAt(slot: Slot) { nestSlot = slot }

    fun discard() {
        password.discard()
        registerDomainEvent(EggDiscarded(this))
    }

    override fun equals(other: Any?) =
        (other as? Egg)?.let { it.viewEggId() == viewEggId() && it.associatedNest() == nestSlot } ?: false
    override fun hashCode() = nestSlot.hashCode() + 31 * eggId.hashCode()

    companion object {
        fun createEgg(slot: Slot, eggIdShell: Shell, passwordShell: Shell) =
            Egg(slot, createEggId(eggIdShell), createPassword(passwordShell))
    }
}
