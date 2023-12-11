package de.pflugradts.passbird.domain.service.password

import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.InvalidKeyException
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.CharValue.Companion.charValueOf
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.EggRepository
import java.util.Optional
import java.util.function.Predicate

abstract class CommonPasswordServiceCapabilities(
    private val cryptoProvider: CryptoProvider,
    private val eggRepository: EggRepository,
    private val eventRegistry: EventRegistry,
) {
    fun find(keyBytes: Bytes, nestSlot: Slot): Optional<Egg> = eggRepository.find(keyBytes, nestSlot)
    fun find(keyBytes: Bytes): Optional<Egg> = eggRepository.find(keyBytes)
    fun encrypted(bytes: Bytes) = cryptoProvider.encrypt(bytes)
    fun decrypted(bytes: Bytes) = cryptoProvider.decrypt(bytes)

    fun processEventsAndSync() {
        eventRegistry.processEvents()
        eggRepository.sync()
    }

    fun challengeAlias(bytes: Bytes) {
        if (charValueOf(bytes.getByte(0)).isDigit || anyMatch(bytes.copy()) { charValueOf(it).isSymbol }) {
            throw InvalidKeyException(bytes)
        }
    }
    private fun anyMatch(bytes: Bytes, predicate: Predicate<Byte>): Boolean {
        val result = bytes.stream().anyMatch(predicate)
        bytes.scramble()
        return result
    }

    fun eggExists(keyBytes: Bytes, nestSlot: Slot) = find(encrypted(keyBytes), nestSlot).isPresent
    fun eggExists(keyBytes: Bytes, eggNotExistsAction: EggNotExistsAction) =
        encrypted(keyBytes).let {
            find(it).let { match ->
                if (match.isEmpty && eggNotExistsAction == EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT) {
                    eventRegistry.register(EggNotFound(it))
                    eventRegistry.processEvents()
                }
                match.isPresent
            }
        }
}
