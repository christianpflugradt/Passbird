package de.pflugradts.passbird.domain.model.password

import de.pflugradts.passbird.domain.model.ddd.AggregateRoot
import de.pflugradts.passbird.domain.model.event.PasswordEntryCreated
import de.pflugradts.passbird.domain.model.event.PasswordEntryDiscarded
import de.pflugradts.passbird.domain.model.event.PasswordEntryRenamed
import de.pflugradts.passbird.domain.model.event.PasswordEntryUpdated
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.password.Key.Companion.createKey
import de.pflugradts.passbird.domain.model.password.Password.Companion.createPassword
import de.pflugradts.passbird.domain.model.transfer.Bytes

/**
 * A PasswordEntry represents a [Key] and an associated [Password] stored in the
 * [PasswordStore][PasswordStoreAdapterPort].
 */
class PasswordEntry private constructor(
    private var nestSlot: Slot,
    private val key: Key,
    private val password: Password,
) : AggregateRoot() {

    init { registerDomainEvent(PasswordEntryCreated(this)) }
    fun associatedNest() = nestSlot
    fun viewKey() = key.view()
    fun viewPassword() = password.view()

    fun rename(keyBytes: Bytes) {
        key.rename(keyBytes)
        registerDomainEvent(PasswordEntryRenamed(this))
    }

    fun updatePassword(bytes: Bytes) {
        password.update(bytes)
        registerDomainEvent(PasswordEntryUpdated(this))
    }

    fun moveToNestAt(slot: Slot) { nestSlot = slot }

    fun discard() {
        password.discard()
        registerDomainEvent(PasswordEntryDiscarded(this))
    }

    override fun equals(other: Any?) =
        (other as? PasswordEntry)?.let { it.viewKey() == viewKey() && it.associatedNest() == nestSlot } ?: false
    override fun hashCode() = nestSlot.hashCode() + 31 * key.hashCode()

    companion object {
        fun createPasswordEntry(slot: Slot, keyBytes: Bytes, passwordBytes: Bytes) =
            PasswordEntry(slot, createKey(keyBytes), createPassword(passwordBytes))
    }
}
