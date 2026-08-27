package de.pflugradts.passbird.domain.service.password

import de.pflugradts.kotlinextensions.Option
import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.kotlinextensions.toOption
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.EggIdMemory
import de.pflugradts.passbird.domain.model.egg.requireValidEggId
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.EggRepository
import java.util.stream.Stream

abstract class CommonPasswordServiceCapabilities(
    private val cryptoProvider: CryptoProvider,
    protected val eggRepository: EggRepository,
    protected val eventRegistry: EventRegistry,
    private val memoryUpdateControl: MemoryUpdateControl,
) {
    fun find(eggIdShell: Shell, slot: Slot): Option<Egg> = eggRepository.findAll(slot).findDecrypted(eggIdShell)
    fun find(eggIdShell: Shell): Option<Egg> = eggRepository.findAll().findDecrypted(eggIdShell).apply {
        ifPresent {
            if (memoryUpdateControl.updatesEnabled()) {
                updateMemory(it)
            }
        }
    }
    fun findWithoutUpdatingMemory(eggIdShell: Shell): Option<Egg> = eggRepository.findAll().findDecrypted(eggIdShell)
    fun findIncludingTrashed(eggIdShell: Shell, slot: Slot): Option<Egg> =
        eggRepository.findAllIncludingTrashed(slot).findDecrypted(eggIdShell)
    fun findTrashed(eggIdShell: Shell): Option<Egg> = eggRepository.findAllTrashed().findDecrypted(eggIdShell)

    private fun Stream<Egg>.findDecrypted(eggIdShell: Shell) = filter {
        decryptedMatches(it.viewEggId(), eggIdShell)
    }.findAny().toOption()
    private fun EggIdMemory.findDuplicate(egg: Egg): EncryptedShell? {
        val eggId = egg.viewEggId()
        return find { entry ->
            entry.map { decryptedMatches(it, eggId) }.orElse(false)
        }?.get()
    }

    private fun decryptedMatches(encryptedShell: EncryptedShell, shell: Shell): Boolean {
        val decryptedShell = decrypted(encryptedShell)
        return try {
            decryptedShell == shell
        } finally {
            decryptedShell.scramble()
        }
    }

    private fun decryptedMatches(firstEncryptedShell: EncryptedShell, secondEncryptedShell: EncryptedShell): Boolean {
        val firstShell = decrypted(firstEncryptedShell)
        val secondShell = decrypted(secondEncryptedShell)
        return try {
            firstShell == secondShell
        } finally {
            firstShell.scramble()
            secondShell.scramble()
        }
    }

    fun updateMemory(egg: Egg, sync: Boolean = true) = eggRepository.updateMemory(egg, eggRepository.memory().findDuplicate(egg), sync)

    fun encrypted(shell: Shell) = cryptoProvider.encrypt(shell)
    fun decrypted(encryptedShell: EncryptedShell) = cryptoProvider.decrypt(encryptedShell)

    fun processEventsAndSync(): TryResult<Unit> = eggRepository.sync()
        .onSuccess { eventRegistry.processEvents() }
        .onFailure { eventRegistry.clearEvents() }
    fun registerEggNotFound(eggIdShell: Shell) {
        eventRegistry.register(EggNotFound(eggIdShell))
        eventRegistry.processEvents()
    }

    fun challengeEggId(shell: Shell) = requireValidEggId(shell)

    fun eggExists(eggIdShell: Shell, slot: Slot) = find(eggIdShell, slot).isPresent
    fun eggExists(eggIdShell: Shell, eggNotExistsAction: EggNotExistsAction) = find(eggIdShell).let {
        if (it.isEmpty && eggNotExistsAction == EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT) {
            eventRegistry.register(EggNotFound(eggIdShell))
            eventRegistry.processEvents()
        }
        it.isPresent
    }
}
