package de.pflugradts.passbird.domain.service.password

import de.pflugradts.kotlinextensions.Option
import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.kotlinextensions.TryResult.Companion.success
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slots
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.EggRepository
import jakarta.inject.Inject

class FavoritePasswordService @Inject constructor(
    cryptoProvider: CryptoProvider,
    eggRepository: EggRepository,
    eventRegistry: EventRegistry,
) : CommonPasswordServiceCapabilities(cryptoProvider, eggRepository, eventRegistry) {
    fun viewFavorites() = Slots<Shell>().apply {
        eggRepository.favorites().forEachIndexed { index, favorite ->
            favorite.map(::decrypted).ifPresent { this[index] = it }
        }
    }

    fun viewFavoriteEntry(slot: Slot): Option<Shell> = eggRepository.favorites()[slot].map(::decrypted)

    fun putFavorite(slot: Slot, eggIdShell: Shell): TryResult<Unit> = findWithoutUpdatingMemory(eggIdShell).let { egg ->
        if (egg.isPresent) {
            eggRepository.putFavorite(slot, egg.get().viewEggId())
            processEventsAndSync()
        } else {
            registerEggNotFound(eggIdShell)
            success(Unit)
        }
    }

    fun discardFavorite(slot: Slot): TryResult<Unit> {
        eggRepository.discardFavorite(slot)
        return processEventsAndSync()
    }
}
