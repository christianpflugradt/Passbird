package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.egg.EggIdAlreadyExistsException
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.EggRepository

class MovePasswordService @Inject constructor(
    cryptoProvider: CryptoProvider,
    eggRepository: EggRepository,
    private val eventRegistry: EventRegistry,
) : CommonPasswordServiceCapabilities(cryptoProvider, eggRepository, eventRegistry) {
    fun movePassword(eggIdShell: Shell, targetSlot: Slot) {
        if (eggExists(eggIdShell, targetSlot)) {
            throw EggIdAlreadyExistsException(eggIdShell)
        } else {
            find(eggIdShell).ifPresentOrElse({ it.moveToNestAt(targetSlot) }, { eventRegistry.register(EggNotFound(eggIdShell)) })
            processEventsAndSync()
        }
    }
}
