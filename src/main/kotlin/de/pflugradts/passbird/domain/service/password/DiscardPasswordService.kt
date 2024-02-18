package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.EggRepository

class DiscardPasswordService @Inject constructor(
    @Inject private val cryptoProvider: CryptoProvider,
    @Inject private val eggRepository: EggRepository,
    @Inject private val eventRegistry: EventRegistry,
) : CommonPasswordServiceCapabilities(cryptoProvider, eggRepository, eventRegistry) {
    fun discardEgg(eggIdShell: Shell) {
        encrypted(eggIdShell).let { encryptedEggIdShell ->
            find(encryptedEggIdShell).ifPresentOrElse(
                { it.discard() },
                { eventRegistry.register(EggNotFound(encryptedEggIdShell)) },
            )
        }
        processEventsAndSync()
    }
}
