package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.event.PasswordEntryNotFound
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.BytesComparator
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository
import java.util.Optional
import java.util.stream.Stream

class ViewPasswordService @Inject constructor(
    @Inject private val cryptoProvider: CryptoProvider,
    @Inject private val passwordEntryRepository: PasswordEntryRepository,
    @Inject private val eventRegistry: EventRegistry,
) : CommonPasswordServiceCapabilities(cryptoProvider, passwordEntryRepository, eventRegistry) {
    fun findAllKeys(): Stream<Bytes> = passwordEntryRepository.findAll().map { decrypted(it.viewKey()) }.sorted(BytesComparator())
    fun viewPassword(keyBytes: Bytes): Optional<Bytes> =
        encrypted(keyBytes).let { encryptedKeyBytes ->
            find(encryptedKeyBytes)
                .map { decrypted(it.viewPassword()) }
                .or {
                    eventRegistry.register(PasswordEntryNotFound(encryptedKeyBytes))
                    eventRegistry.processEvents()
                    Optional.empty()
                }
        }
}
