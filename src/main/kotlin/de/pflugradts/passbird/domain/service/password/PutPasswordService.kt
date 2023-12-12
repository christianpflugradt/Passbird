package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.BytePair
import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.service.NestService
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.EggRepository
import java.util.stream.Stream

class PutPasswordService @Inject constructor(
    @Inject private val cryptoProvider: CryptoProvider,
    @Inject private val eggRepository: EggRepository,
    @Inject private val eventRegistry: EventRegistry,
    @Inject private val nestService: NestService,
) : CommonPasswordServiceCapabilities(cryptoProvider, eggRepository, eventRegistry) {
    fun putEggs(eggs: Stream<BytePair>) {
        eggs.forEach { putEgg(it.value.first, it.value.second, false) }
        processEventsAndSync()
    }

    fun putEgg(eggIdBytes: Bytes, passwordBytes: Bytes, sync: Boolean = true) {
        challengeEggId(eggIdBytes)
        val encryptedEggIdBytes = encrypted(eggIdBytes)
        val encryptedPasswordBytes = encrypted(passwordBytes)
        val nestSlot = nestService.getCurrentNest().slot
        find(encryptedEggIdBytes).ifPresentOrElse(
            { it.updatePassword(encryptedPasswordBytes) },
            { eggRepository.add(createEgg(nestSlot, encryptedEggIdBytes, encryptedPasswordBytes)) },
        )
        if (sync) processEventsAndSync()
    }
}
