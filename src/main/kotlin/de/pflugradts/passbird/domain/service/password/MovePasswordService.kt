package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.egg.KeyAlreadyExistsException
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.EggRepository

class MovePasswordService @Inject constructor(
    @Inject private val cryptoProvider: CryptoProvider,
    @Inject private val eggRepository: EggRepository,
    @Inject private val eventRegistry: EventRegistry,
) : CommonPasswordServiceCapabilities(cryptoProvider, eggRepository, eventRegistry) {
    fun movePassword(keyBytes: Bytes, targetNestSlot: Slot) {
        if (eggExists(keyBytes, targetNestSlot)) {
            throw KeyAlreadyExistsException(keyBytes)
        } else {
            encrypted(keyBytes).let { encryptedKeyBytes ->
                find(encryptedKeyBytes).ifPresentOrElse(
                    { it.moveToNestAt(targetNestSlot) },
                    { eventRegistry.register(EggNotFound(encryptedKeyBytes)) },
                )
            }
            processEventsAndSync()
        }
    }
}
