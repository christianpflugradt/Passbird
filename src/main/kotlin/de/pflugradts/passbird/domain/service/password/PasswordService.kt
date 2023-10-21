package de.pflugradts.passbird.domain.service.password

import de.pflugradts.passbird.domain.model.BytePair
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.transfer.Bytes
import java.util.Optional
import java.util.stream.Stream

/**
 *
 * A PasswordServices manages [PasswordEntries][de.pflugradts.passbird.domain.model.password.PasswordEntry].
 *
 * PasswordEntries can be viewed, created, updated or removed through methods such as [.viewPassword],
 * [.putPasswordEntry] and [.discardPasswordEntry].
 */
interface PasswordService {
    enum class EntryNotExistsAction {
        DO_NOTHING,
        CREATE_ENTRY_NOT_EXISTS_EVENT,
    }

    fun entryExists(keyBytes: Bytes, namespace: NamespaceSlot): Boolean
    fun entryExists(keyBytes: Bytes, entryNotExistsAction: EntryNotExistsAction): Boolean
    fun viewPassword(keyBytes: Bytes): Optional<Bytes>
    fun renamePasswordEntry(keyBytes: Bytes, newKeyBytes: Bytes)
    fun challengeAlias(bytes: Bytes)
    fun putPasswordEntries(passwordEntries: Stream<BytePair>)
    fun putPasswordEntry(keyBytes: Bytes, passwordBytes: Bytes)
    fun discardPasswordEntry(keyBytes: Bytes)
    fun movePasswordEntry(keyBytes: Bytes, targetNamespace: NamespaceSlot)
    fun findAllKeys(): Stream<Bytes>
}
