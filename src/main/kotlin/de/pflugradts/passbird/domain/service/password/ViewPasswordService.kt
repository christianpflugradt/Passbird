package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.event.PasswordEntryNotFound
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.BytesComparator
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.PasswordService.EntryNotExistsAction
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository
import java.util.Optional
import java.util.stream.Stream

class ViewPasswordService @Inject constructor(
    @Inject private val cryptoProvider: CryptoProvider,
    @Inject private val passwordEntryRepository: PasswordEntryRepository,
    @Inject private val eventRegistry: EventRegistry,
) : CommonPasswordServiceCapabilities {
    fun entryExists(keyBytes: Bytes, namespace: NamespaceSlot): Boolean =
        entryExists(cryptoProvider, passwordEntryRepository, keyBytes, namespace)
    fun entryExists(keyBytes: Bytes, entryNotExistsAction: EntryNotExistsAction): Boolean =
        entryExists(cryptoProvider, passwordEntryRepository, eventRegistry, keyBytes, entryNotExistsAction)

    fun viewPassword(keyBytes: Bytes): Optional<Bytes> =
        encrypted(cryptoProvider, keyBytes).let { encryptedKeyBytes ->
            find(passwordEntryRepository, encryptedKeyBytes)
                .map { decrypted(cryptoProvider, it.viewPassword()) }
                .or {
                    eventRegistry.register(PasswordEntryNotFound(encryptedKeyBytes))
                    eventRegistry.processEvents()
                    Optional.empty()
                }
        }

    fun findAllKeys(): Stream<Bytes> =
        passwordEntryRepository.findAll().map { decrypted(cryptoProvider, it.viewKey()) }.sorted(BytesComparator())
}
