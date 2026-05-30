package de.pflugradts.passbird.domain.service.password
import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.EggRepository
class DiscardPasswordService constructor(
    cryptoProvider: CryptoProvider,
    eggRepository: EggRepository,
    eventRegistry: EventRegistry,
) : CommonPasswordServiceCapabilities(cryptoProvider, eggRepository, eventRegistry) {
    fun discardEgg(eggIdShell: Shell): TryResult<Unit> = findWithoutUpdatingMemory(eggIdShell)
        .ifPresentOrElse(
            {
                eggRepository.discardFavorites(it.associatedNest(), it.viewEggId())
                it.discard()
            },
            { eventRegistry.register(EggNotFound(eggIdShell)) },
        )
        .let { processEventsAndSync() }
    fun discardProtein(eggIdShell: Shell, slot: Slot): TryResult<Unit> = findWithoutUpdatingMemory(eggIdShell)
        .ifPresentOrElse({ it.discardProtein(slot)}, { eventRegistry.register(EggNotFound(eggIdShell)) })
        .let { processEventsAndSync() }
}
