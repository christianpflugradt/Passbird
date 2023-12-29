package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.kotlinextensions.MutableOption.Companion.emptyOption
import de.pflugradts.kotlinextensions.Option
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.ShellComparator
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.EggRepository
import java.util.stream.Stream

class ViewPasswordService @Inject constructor(
    @Inject private val cryptoProvider: CryptoProvider,
    @Inject private val eggRepository: EggRepository,
    @Inject private val eventRegistry: EventRegistry,
) : CommonPasswordServiceCapabilities(cryptoProvider, eggRepository, eventRegistry) {
    fun findAllEggIds(): Stream<Shell> = eggRepository.findAll().map { decrypted(it.viewEggId()) }.sorted(ShellComparator())
    fun viewPassword(eggIdShell: Shell): Option<Shell> =
        encrypted(eggIdShell).let { encryptedEggIdShell ->
            find(encryptedEggIdShell)
                .map { decrypted(it.viewPassword()) }
                .or {
                    eventRegistry.register(EggNotFound(encryptedEggIdShell))
                    eventRegistry.processEvents()
                    emptyOption()
                }
        }
}
