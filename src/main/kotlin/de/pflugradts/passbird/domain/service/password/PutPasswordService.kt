package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.BytePair
import de.pflugradts.passbird.domain.model.password.InvalidKeyException
import de.pflugradts.passbird.domain.model.password.PasswordEntry.Companion.createPasswordEntry
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.CharValue.Companion.charValueOf
import de.pflugradts.passbird.domain.service.NamespaceService
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository
import java.util.function.Predicate
import java.util.stream.Stream

class PutPasswordService @Inject constructor(
    @Inject private val cryptoProvider: CryptoProvider,
    @Inject private val passwordEntryRepository: PasswordEntryRepository,
    @Inject private val eventRegistry: EventRegistry,
    @Inject private val namespaceService: NamespaceService,
) : CommonPasswordServiceCapabilities {
    fun putPasswordEntries(passwordEntries: Stream<BytePair>) {
        passwordEntries.forEach { putPasswordEntry(it.value.first, it.value.second, false) }
        processEventsAndSync(eventRegistry, passwordEntryRepository)
    }

    fun putPasswordEntry(keyBytes: Bytes, passwordBytes: Bytes, sync: Boolean = true) {
        putPasswordEntryPair(BytePair(Pair(keyBytes, passwordBytes)))
        if (sync) processEventsAndSync(eventRegistry, passwordEntryRepository)
    }

    private fun putPasswordEntryPair(passwordEntryPair: BytePair) {
        challengeAlias(passwordEntryPair.value.first)
        putEncryptedPasswordEntry(
            encrypted(cryptoProvider, passwordEntryPair.value.first),
            encrypted(cryptoProvider, passwordEntryPair.value.second),
        )
    }

    private fun putEncryptedPasswordEntry(encryptedKeyBytes: Bytes, encryptedPasswordBytes: Bytes) {
        find(passwordEntryRepository, encryptedKeyBytes).ifPresentOrElse(
            { it.updatePassword(encryptedPasswordBytes) },
            {
                passwordEntryRepository.add(
                    createPasswordEntry(namespaceService.getCurrentNamespace().slot, encryptedKeyBytes, encryptedPasswordBytes),
                )
            },
        )
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
}
