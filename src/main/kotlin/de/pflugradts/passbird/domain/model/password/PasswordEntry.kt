package de.pflugradts.passbird.domain.model.password

import de.pflugradts.passbird.domain.model.ddd.AggregateRoot
import de.pflugradts.passbird.domain.model.event.PasswordEntryCreated
import de.pflugradts.passbird.domain.model.event.PasswordEntryDiscarded
import de.pflugradts.passbird.domain.model.event.PasswordEntryRenamed
import de.pflugradts.passbird.domain.model.event.PasswordEntryUpdated
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.password.Key.Companion.createKey
import de.pflugradts.passbird.domain.model.password.Password.Companion.createPassword
import de.pflugradts.passbird.domain.model.transfer.Bytes

/**
 * A PasswordEntry represents a [Key] and an associated [Password] stored in the
 * [PasswordStore][PasswordStoreAdapterPort].
 */
class PasswordEntry private constructor(
    private var namespace: NamespaceSlot,
    private val key: Key,
    private val password: Password,
) : AggregateRoot() {

    init { registerDomainEvent(PasswordEntryCreated(this)) }
    fun associatedNamespace() = namespace
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

    fun updateNamespace(namespaceSlot: NamespaceSlot) { namespace = namespaceSlot }

    fun discard() {
        password.discard()
        registerDomainEvent(PasswordEntryDiscarded(this))
    }

    override fun equals(other: Any?) =
        (other as? PasswordEntry)?.let { it.viewKey() == viewKey() && it.associatedNamespace() == namespace } ?: false
    override fun hashCode() = namespace.hashCode() + 31 * key.hashCode()

    companion object {
        fun createPasswordEntry(namespaceSlot: NamespaceSlot, keyBytes: Bytes, passwordBytes: Bytes) =
            PasswordEntry(namespaceSlot, createKey(keyBytes), createPassword(passwordBytes))
    }
}
