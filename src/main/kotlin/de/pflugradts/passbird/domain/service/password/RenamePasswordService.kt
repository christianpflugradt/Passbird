package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.egg.EggIdAlreadyExistsException
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.EggRepository

class RenamePasswordService @Inject constructor(
    @Inject private val cryptoProvider: CryptoProvider,
    @Inject private val eggRepository: EggRepository,
    @Inject private val eventRegistry: EventRegistry,
) : CommonPasswordServiceCapabilities(cryptoProvider, eggRepository, eventRegistry) {
    fun renameEgg(eggIdBytes: Bytes, newEggIdBytes: Bytes) {
        challengeEggId(newEggIdBytes)
        if (eggExists(eggIdBytes, CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            encrypted(newEggIdBytes).let { encryptedNewEggIdBytes ->
                if (find(encryptedNewEggIdBytes).isEmpty) {
                    encrypted(eggIdBytes).let { find(it).get().rename(newEggIdBytes) }
                    processEventsAndSync()
                } else {
                    throw EggIdAlreadyExistsException(newEggIdBytes)
                }
            }
        }
    }
}
