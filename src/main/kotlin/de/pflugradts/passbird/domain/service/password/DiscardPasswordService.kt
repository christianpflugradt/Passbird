package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.event.PasswordEntryNotFound
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository

class DiscardPasswordService @Inject constructor(
    @Inject private val cryptoProvider: CryptoProvider,
    @Inject private val passwordEntryRepository: PasswordEntryRepository,
    @Inject private val eventRegistry: EventRegistry,
) : CommonPasswordServiceCapabilities {
    fun discardPasswordEntry(keyBytes: Bytes) {
        encrypted(cryptoProvider, keyBytes).let { encryptedKeyBytes ->
            find(passwordEntryRepository, encryptedKeyBytes).ifPresentOrElse(
                { it.discard() },
                { eventRegistry.register(PasswordEntryNotFound(encryptedKeyBytes)) },
            )
        }
        processEventsAndSync(eventRegistry, passwordEntryRepository)
    }
}
