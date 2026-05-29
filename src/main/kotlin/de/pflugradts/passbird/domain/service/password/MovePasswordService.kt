package de.pflugradts.passbird.domain.service.password

import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.passbird.domain.model.egg.EggIdAlreadyExistsException
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.EggRepository
import jakarta.inject.Inject

class MovePasswordService @Inject constructor(
    cryptoProvider: CryptoProvider,
    eggRepository: EggRepository,
    eventRegistry: EventRegistry,
) : CommonPasswordServiceCapabilities(cryptoProvider, eggRepository, eventRegistry) {
    fun movePassword(eggIdShell: Shell, targetSlot: Slot): TryResult<Unit> {
        if (eggExists(eggIdShell, targetSlot)) {
            throw EggIdAlreadyExistsException(eggIdShell)
        } else {
            findWithoutUpdatingMemory(eggIdShell).ifPresentOrElse(
                {
                    eggRepository.discardFavorites(it.associatedNest(), it.viewEggId())
                    it.moveToNestAt(targetSlot)
                },
                { eventRegistry.register(EggNotFound(eggIdShell)) },
            )
            return processEventsAndSync()
        }
    }
}
