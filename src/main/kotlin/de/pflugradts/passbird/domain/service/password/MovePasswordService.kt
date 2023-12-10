package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.event.PasswordEntryNotFound
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.password.KeyAlreadyExistsException
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository

class MovePasswordService @Inject constructor(
    @Inject private val cryptoProvider: CryptoProvider,
    @Inject private val passwordEntryRepository: PasswordEntryRepository,
    @Inject private val eventRegistry: EventRegistry,
) : CommonPasswordServiceCapabilities(cryptoProvider, passwordEntryRepository, eventRegistry) {
    fun movePassword(keyBytes: Bytes, targetNestSlot: Slot) {
        if (entryExists(keyBytes, targetNestSlot)) {
            throw KeyAlreadyExistsException(keyBytes)
        } else {
            encrypted(keyBytes).let { encryptedKeyBytes ->
                find(encryptedKeyBytes).ifPresentOrElse(
                    { it.moveToNestAt(targetNestSlot) },
                    { eventRegistry.register(PasswordEntryNotFound(encryptedKeyBytes)) },
                )
            }
            processEventsAndSync()
        }
    }
}
