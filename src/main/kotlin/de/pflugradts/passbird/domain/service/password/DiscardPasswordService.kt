package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.EggRepository

class DiscardPasswordService @Inject constructor(
    cryptoProvider: CryptoProvider,
    eggRepository: EggRepository,
    private val eventRegistry: EventRegistry,
) : CommonPasswordServiceCapabilities(cryptoProvider, eggRepository, eventRegistry) {
    fun discardEgg(eggIdShell: Shell) = find(eggIdShell)
        .ifPresentOrElse({ it.discard() }, { eventRegistry.register(EggNotFound(eggIdShell)) })
        .also { processEventsAndSync() }
}
