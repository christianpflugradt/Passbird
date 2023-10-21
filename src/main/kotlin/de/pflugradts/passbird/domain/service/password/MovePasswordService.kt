package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.event.PasswordEntryNotFound
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.password.KeyAlreadyExistsException
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository

class MovePasswordService @Inject constructor(
    @Inject private val cryptoProvider: CryptoProvider,
    @Inject private val passwordEntryRepository: PasswordEntryRepository,
    @Inject private val eventRegistry: EventRegistry,
) : CommonPasswordServiceCapabilities {
    fun movePassword(keyBytes: Bytes, targetNamespace: NamespaceSlot) {
        if (entryExists(cryptoProvider, passwordEntryRepository, keyBytes, targetNamespace)) {
            throw KeyAlreadyExistsException(keyBytes)
        } else {
            encrypted(cryptoProvider, keyBytes).let { encryptedKeyBytes ->
                find(passwordEntryRepository, encryptedKeyBytes).ifPresentOrElse(
                    { it.updateNamespace(targetNamespace) },
                    { eventRegistry.register(PasswordEntryNotFound(encryptedKeyBytes)) },
                )
            }
            processEventsAndSync(eventRegistry, passwordEntryRepository)
        }
    }
}
