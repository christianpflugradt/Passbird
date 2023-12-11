package de.pflugradts.passbird.domain.model.egg

import de.pflugradts.passbird.domain.model.ddd.AggregateRoot
import de.pflugradts.passbird.domain.model.egg.Key.Companion.createKey
import de.pflugradts.passbird.domain.model.egg.Password.Companion.createPassword
import de.pflugradts.passbird.domain.model.event.EggCreated
import de.pflugradts.passbird.domain.model.event.EggDiscarded
import de.pflugradts.passbird.domain.model.event.EggRenamed
import de.pflugradts.passbird.domain.model.event.EggUpdated
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.transfer.Bytes

/**
 * An Egg represents an [EggName] and an associated [Password] stored in the
 * [PasswordStore][PasswordStoreAdapterPort].
 */
class Egg private constructor(
    private var nestSlot: Slot,
    private val key: Key,
    private val password: Password,
) : AggregateRoot() {

    init { registerDomainEvent(EggCreated(this)) }
    fun associatedNest() = nestSlot
    fun viewKey() = key.view()
    fun viewPassword() = password.view()

    fun rename(keyBytes: Bytes) {
        key.rename(keyBytes)
        registerDomainEvent(EggRenamed(this))
    }

    fun updatePassword(bytes: Bytes) {
        password.update(bytes)
        registerDomainEvent(EggUpdated(this))
    }

    fun moveToNestAt(slot: Slot) { nestSlot = slot }

    fun discard() {
        password.discard()
        registerDomainEvent(EggDiscarded(this))
    }

    override fun equals(other: Any?) =
        (other as? Egg)?.let { it.viewKey() == viewKey() && it.associatedNest() == nestSlot } ?: false
    override fun hashCode() = nestSlot.hashCode() + 31 * key.hashCode()

    companion object {
        fun createEgg(slot: Slot, keyBytes: Bytes, passwordBytes: Bytes) =
            Egg(slot, createKey(keyBytes), createPassword(passwordBytes))
    }
}
