package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.password.KeyAlreadyExistsException
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.PasswordService.EntryNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository

class RenamePasswordService @Inject constructor(
    @Inject private val cryptoProvider: CryptoProvider,
    @Inject private val passwordEntryRepository: PasswordEntryRepository,
    @Inject private val eventRegistry: EventRegistry,
) : CommonPasswordServiceCapabilities(cryptoProvider, passwordEntryRepository, eventRegistry) {
    fun renamePasswordEntry(keyBytes: Bytes, newKeyBytes: Bytes) {
        if (entryExists(keyBytes, CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            encrypted(newKeyBytes).let { encryptedNewKeyBytes ->
                if (find(encryptedNewKeyBytes).isEmpty) {
                    encrypted(keyBytes).let { find(it).get().rename(newKeyBytes) }
                    processEventsAndSync()
                } else {
                    throw KeyAlreadyExistsException(newKeyBytes)
                }
            }
        }
    }
}
