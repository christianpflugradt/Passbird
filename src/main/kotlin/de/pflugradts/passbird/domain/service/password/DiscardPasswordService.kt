package de.pflugradts.passbird.domain.service.password
import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.kotlinextensions.TryResult.Companion.success
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.EggRepository
class DiscardPasswordService constructor(
    cryptoProvider: CryptoProvider,
    eggRepository: EggRepository,
    eventRegistry: EventRegistry,
    memoryUpdateControl: MemoryUpdateControl,
    private val trashRetentionDaysSupplier: () -> Int,
    private val nestService: NestService,
    private val currentEpochDaySupplier: () -> Int,
) : CommonPasswordServiceCapabilities(cryptoProvider, eggRepository, eventRegistry, memoryUpdateControl) {
    fun discardEgg(eggIdShell: Shell, currentEpochDay: Int): TryResult<Unit> = findWithoutUpdatingMemory(eggIdShell)
        .ifPresentOrElse(
            {
                eggRepository.discardFavorites(it.associatedNest(), it.viewEggId())
                eggRepository.discardMemory(it.associatedNest(), it.viewEggId())
                it.trash(currentEpochDay)
            },
            { eventRegistry.register(EggNotFound(eggIdShell)) },
        )
        .let { processEventsAndSync() }
    fun discardEggPermanently(eggIdShell: Shell): TryResult<Unit> = findIncludingTrashed(eggIdShell, nestService.currentNest().slot)
        .ifPresentOrElse(
            {
                eggRepository.discardFavorites(it.associatedNest(), it.viewEggId())
                eggRepository.discardMemory(it.associatedNest(), it.viewEggId())
                it.discard()
            },
            { eventRegistry.register(EggNotFound(eggIdShell)) },
        )
        .let { processEventsAndSync() }
    fun restoreEgg(eggIdShell: Shell): TryResult<RestoreEggResult> = findTrashed(eggIdShell).map { egg ->
        val fallbackToDefault = egg.associatedNest() != Slot.DEFAULT && nestService.atNestSlot(egg.associatedNest()).isEmpty
        val targetSlot = if (fallbackToDefault) {
            Slot.DEFAULT
        } else {
            egg.associatedNest()
        }
        val currentNestSnapshot = nestService.currentNest().slot
        if (eggExists(eggIdShell, targetSlot)) {
            success(RestoreEggResult.TARGET_CONFLICT)
        } else {
            egg.restore(targetSlot)
            try {
                nestService.moveToNestAt(targetSlot)
                updateMemory(egg, sync = false)
                processEventsAndSync().map {
                    if (fallbackToDefault) {
                        RestoreEggResult.RESTORED_TO_DEFAULT
                    } else {
                        RestoreEggResult.RESTORED
                    }
                }
            } finally {
                nestService.moveToNestAt(currentNestSnapshot)
            }
        }
    }.orElse(success(RestoreEggResult.NOT_FOUND))
    fun cleanupTrash(): TryResult<Int> {
        val thresholdEpochDay = currentEpochDaySupplier() - trashRetentionDaysSupplier()
        val trashedEggs = eggRepository.findAllTrashed().toList()
        var discarded = 0
        trashedEggs.forEach { egg ->
            if (thresholdEpochDay > egg.deletionEpochDay()) {
                egg.discard()
                discarded += 1
            }
        }
        return if (discarded > 0) {
            processEventsAndSync().map { discarded }
        } else {
            success(0)
        }
    }
    fun discardProtein(eggIdShell: Shell, slot: Slot): TryResult<Unit> = findWithoutUpdatingMemory(eggIdShell)
        .ifPresentOrElse({ it.discardProtein(slot)}, { eventRegistry.register(EggNotFound(eggIdShell)) })
        .let { processEventsAndSync() }
    fun discardYolk(eggIdShell: Shell): TryResult<Unit> = findWithoutUpdatingMemory(eggIdShell)
        .ifPresentOrElse({ it.discardYolk() }, { eventRegistry.register(EggNotFound(eggIdShell)) })
        .let { processEventsAndSync() }
}
