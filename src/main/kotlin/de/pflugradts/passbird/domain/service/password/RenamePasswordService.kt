package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.event.PasswordEntryNotFound
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
) : CommonPasswordServiceCapabilities {
    fun renamePasswordEntry(keyBytes: Bytes, newKeyBytes: Bytes) {
        if (entryExists(cryptoProvider, passwordEntryRepository, eventRegistry, keyBytes, CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            encrypted(cryptoProvider, newKeyBytes).let { encryptedNewKeyBytes ->
                if (find(passwordEntryRepository, encryptedNewKeyBytes).isEmpty) {
                    renamePasswordEntryOrFail(keyBytes, encryptedNewKeyBytes)
                    processEventsAndSync(eventRegistry, passwordEntryRepository)
                } else {
                    throw KeyAlreadyExistsException(newKeyBytes)
                }
            }
        }
    }

    private fun renamePasswordEntryOrFail(keyBytes: Bytes, newKeyBytes: Bytes) =
        encrypted(cryptoProvider, keyBytes).let { encryptedKeyBytes ->
            find(passwordEntryRepository, encryptedKeyBytes).ifPresentOrElse(
                { it.rename(newKeyBytes) },
                { eventRegistry.register(PasswordEntryNotFound(encryptedKeyBytes)) },
            )
        }
}
