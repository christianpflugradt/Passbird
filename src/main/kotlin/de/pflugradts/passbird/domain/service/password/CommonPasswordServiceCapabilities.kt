package de.pflugradts.passbird.domain.service.password

import de.pflugradts.kotlinextensions.Option
import de.pflugradts.passbird.domain.model.egg.Egg
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

abstract class CommonPasswordServiceCapabilities(
    private val cryptoProvider: CryptoProvider,
    private val eggRepository: EggRepository,
    private val eventRegistry: EventRegistry,
) {
    fun find(eggIdShell: Shell, slot: Slot): Option<Egg> = eggRepository.find(eggIdShell, slot)
    fun find(eggIdShell: Shell): Option<Egg> = eggRepository.find(eggIdShell)
    fun encrypted(shell: Shell) = cryptoProvider.encrypt(shell)
    fun decrypted(encryptedShell: EncryptedShell) = cryptoProvider.decrypt(encryptedShell)

    fun processEventsAndSync() {
        eventRegistry.processEvents()
        eggRepository.sync()
    }

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

    fun eggExists(eggIdShell: Shell, slot: Slot) = find(encrypted(eggIdShell).payload, slot).isPresent
    fun eggExists(eggIdShell: Shell, eggNotExistsAction: EggNotExistsAction) = encrypted(eggIdShell).let {
        find(it.payload).let { match ->
            if (match.isEmpty && eggNotExistsAction == EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT) {
                eventRegistry.register(EggNotFound(it))
                eventRegistry.processEvents()
            }
            match.isPresent
        }
    }
}
