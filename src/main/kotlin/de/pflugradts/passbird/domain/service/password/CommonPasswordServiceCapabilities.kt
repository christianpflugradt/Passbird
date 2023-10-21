package de.pflugradts.passbird.domain.service.password

import de.pflugradts.passbird.domain.model.event.PasswordEntryNotFound
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.password.PasswordEntry
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.PasswordService.EntryNotExistsAction
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository
import java.util.Optional

abstract class CommonPasswordServiceCapabilities(
    private val cryptoProvider: CryptoProvider,
    private val passwordEntryRepository: PasswordEntryRepository,
    private val eventRegistry: EventRegistry,
) {
    fun find(keyBytes: Bytes, namespace: NamespaceSlot): Optional<PasswordEntry> = passwordEntryRepository.find(keyBytes, namespace)
    fun find(keyBytes: Bytes): Optional<PasswordEntry> = passwordEntryRepository.find(keyBytes)
    fun encrypted(bytes: Bytes) = cryptoProvider.encrypt(bytes)
    fun decrypted(bytes: Bytes) = cryptoProvider.decrypt(bytes)

    fun processEventsAndSync() {
        eventRegistry.processEvents()
        passwordEntryRepository.sync()
    }

    fun entryExists(keyBytes: Bytes, namespace: NamespaceSlot) = find(encrypted(keyBytes), namespace).isPresent
    fun entryExists(keyBytes: Bytes, entryNotExistsAction: EntryNotExistsAction) =
        encrypted(keyBytes).let {
            find(it).let { match ->
                if (match.isEmpty && entryNotExistsAction == EntryNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT) {
                    eventRegistry.register(PasswordEntryNotFound(it))
                    eventRegistry.processEvents()
                }
                match.isPresent
            }
        }
}
