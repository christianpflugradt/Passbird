package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.egg.EggIdAlreadyExistsException
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
    fun movePassword(eggIdBytes: Bytes, targetNestSlot: Slot) {
        if (eggExists(eggIdBytes, targetNestSlot)) {
            throw EggIdAlreadyExistsException(eggIdBytes)
        } else {
            encrypted(eggIdBytes).let { encryptedEggIdBytes ->
                find(encryptedEggIdBytes).ifPresentOrElse(
                    { it.moveToNestAt(targetNestSlot) },
                    { eventRegistry.register(EggNotFound(encryptedEggIdBytes)) },
                )
            }
            processEventsAndSync()
        }
    }
}
