package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.BytesComparator
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.EggRepository
import java.util.Optional
import java.util.stream.Stream

class ViewPasswordService @Inject constructor(
    @Inject private val cryptoProvider: CryptoProvider,
    @Inject private val eggRepository: EggRepository,
    @Inject private val eventRegistry: EventRegistry,
) : CommonPasswordServiceCapabilities(cryptoProvider, eggRepository, eventRegistry) {
    fun findAllEggIds(): Stream<Bytes> = eggRepository.findAll().map { decrypted(it.viewEggId()) }.sorted(BytesComparator())
    fun viewPassword(eggIdBytes: Bytes): Optional<Bytes> =
        encrypted(eggIdBytes).let { encryptedEggIdBytes ->
            find(encryptedEggIdBytes)
                .map { decrypted(it.viewPassword()) }
                .or {
                    eventRegistry.register(EggNotFound(encryptedEggIdBytes))
                    eventRegistry.processEvents()
                    Optional.empty()
                }
        }
}
