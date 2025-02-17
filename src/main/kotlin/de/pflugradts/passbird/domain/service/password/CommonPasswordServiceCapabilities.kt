package de.pflugradts.passbird.domain.service.password

import de.pflugradts.kotlinextensions.Option
import de.pflugradts.kotlinextensions.toOption
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.EggIdMemory
import de.pflugradts.passbird.domain.model.egg.InvalidEggIdException
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.shell.PlainValue.Companion.plainValueOf
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.EggRepository
import java.util.function.Predicate
import java.util.stream.Stream

abstract class CommonPasswordServiceCapabilities(
    private val cryptoProvider: CryptoProvider,
    private val eggRepository: EggRepository,
    private val eventRegistry: EventRegistry,
) {
    fun find(eggIdShell: Shell, slot: Slot): Option<Egg> = eggRepository.findAll(slot).findDecrypted(eggIdShell)
    fun find(eggIdShell: Shell): Option<Egg> = eggRepository.findAll().findDecrypted(eggIdShell).apply { ifPresent { updateMemory(it) } }

    private fun Stream<Egg>.findDecrypted(eggIdShell: Shell) = filter { decrypted(it.viewEggId()) == eggIdShell }.findAny().toOption()
    private fun EggIdMemory.findDuplicate(egg: Egg) = find { entry ->
        entry.map { decrypted(it) == decrypted(egg.viewEggId()) }.orElse(false)
    }?.get()

    private fun updateMemory(egg: Egg) = eggRepository.updateMemory(egg, eggRepository.memory().findDuplicate(egg))

    fun encrypted(shell: Shell) = cryptoProvider.encrypt(shell)
    fun decrypted(encryptedShell: EncryptedShell) = cryptoProvider.decrypt(encryptedShell)

    fun processEventsAndSync() = eventRegistry.processEvents().apply { eggRepository.sync() }

    fun challengeEggId(shell: Shell) {
        if (plainValueOf(shell.getByte(0)).isDigit || anyMatch(shell.copy()) { plainValueOf(it).isSymbol }) {
            throw InvalidEggIdException(shell)
        }
    }
    private fun anyMatch(shell: Shell, predicate: Predicate<Byte>): Boolean {
        val result = shell.stream().anyMatch(predicate)
        shell.scramble()
        return result
    }

    fun eggExists(eggIdShell: Shell, slot: Slot) = find(eggIdShell, slot).isPresent
    fun eggExists(eggIdShell: Shell, eggNotExistsAction: EggNotExistsAction) = find(eggIdShell).let {
        if (it.isEmpty && eggNotExistsAction == EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT) {
            eventRegistry.register(EggNotFound(eggIdShell))
            eventRegistry.processEvents()
        }
        it.isPresent
    }
}
