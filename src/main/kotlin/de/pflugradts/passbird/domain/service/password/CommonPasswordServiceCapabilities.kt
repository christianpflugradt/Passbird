package de.pflugradts.passbird.domain.service.password

import de.pflugradts.passbird.domain.model.event.PasswordEntryNotFound
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.password.InvalidKeyException
import de.pflugradts.passbird.domain.model.password.PasswordEntry
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.CharValue.Companion.charValueOf
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.PasswordService.EntryNotExistsAction
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository
import java.util.Optional
import java.util.function.Predicate

abstract class CommonPasswordServiceCapabilities(
    private val cryptoProvider: CryptoProvider,
    private val passwordEntryRepository: PasswordEntryRepository,
    private val eventRegistry: EventRegistry,
) {
    fun find(keyBytes: Bytes, nestSlot: Slot): Optional<PasswordEntry> = passwordEntryRepository.find(keyBytes, nestSlot)
    fun find(keyBytes: Bytes): Optional<PasswordEntry> = passwordEntryRepository.find(keyBytes)
    fun encrypted(bytes: Bytes) = cryptoProvider.encrypt(bytes)
    fun decrypted(bytes: Bytes) = cryptoProvider.decrypt(bytes)

    fun processEventsAndSync() {
        eventRegistry.processEvents()
        passwordEntryRepository.sync()
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

    fun entryExists(keyBytes: Bytes, nestSlot: Slot) = find(encrypted(keyBytes), nestSlot).isPresent
    fun entryExists(keyBytes: Bytes, entryNotExistsAction: EntryNotExistsAction) =
        encrypted(keyBytes).let {
            find(it).let { match ->
                if (match.isEmpty && entryNotExistsAction == EntryNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT) {
                    eventRegistry.register(PasswordEntryNotFound(it))
                    eventRegistry.processEvents()
                }
                match.isPresent
            }
        }
}
