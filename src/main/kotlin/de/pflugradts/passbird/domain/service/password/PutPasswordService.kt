package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.BytePair
import de.pflugradts.passbird.domain.model.password.PasswordEntry.Companion.createPasswordEntry
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.service.NestService
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository
import java.util.stream.Stream

class PutPasswordService @Inject constructor(
    @Inject private val cryptoProvider: CryptoProvider,
    @Inject private val passwordEntryRepository: PasswordEntryRepository,
    @Inject private val eventRegistry: EventRegistry,
    @Inject private val nestService: NestService,
) : CommonPasswordServiceCapabilities(cryptoProvider, passwordEntryRepository, eventRegistry) {
    fun putPasswordEntries(passwordEntries: Stream<BytePair>) {
        passwordEntries.forEach { putPasswordEntry(it.value.first, it.value.second, false) }
        processEventsAndSync()
    }

    fun putPasswordEntry(keyBytes: Bytes, passwordBytes: Bytes, sync: Boolean = true) {
        challengeAlias(keyBytes)
        val encryptedKeyBytes = encrypted(keyBytes)
        val encryptedPasswordBytes = encrypted(passwordBytes)
        val nestSlot = nestService.getCurrentNest().slot
        find(encryptedKeyBytes).ifPresentOrElse(
            { it.updatePassword(encryptedPasswordBytes) },
            { passwordEntryRepository.add(createPasswordEntry(nestSlot, encryptedKeyBytes, encryptedPasswordBytes)) },
        )
        if (sync) processEventsAndSync()
    }
}
