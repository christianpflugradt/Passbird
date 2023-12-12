package de.pflugradts.passbird.domain.service.password

import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.InvalidEggIdException
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.shell.PlainValue.Companion.plainValueOf
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.EggRepository
import java.util.Optional
import java.util.function.Predicate

abstract class CommonPasswordServiceCapabilities(
    private val cryptoProvider: CryptoProvider,
    private val eggRepository: EggRepository,
    private val eventRegistry: EventRegistry,
) {
    fun find(eggIdShell: Shell, nestSlot: Slot): Optional<Egg> = eggRepository.find(eggIdShell, nestSlot)
    fun find(eggIdShell: Shell): Optional<Egg> = eggRepository.find(eggIdShell)
    fun encrypted(shell: Shell) = cryptoProvider.encrypt(shell)
    fun decrypted(shell: Shell) = cryptoProvider.decrypt(shell)

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

    fun eggExists(eggIdShell: Shell, nestSlot: Slot) = find(encrypted(eggIdShell), nestSlot).isPresent
    fun eggExists(eggIdShell: Shell, eggNotExistsAction: EggNotExistsAction) =
        encrypted(eggIdShell).let {
            find(it).let { match ->
                if (match.isEmpty && eggNotExistsAction == EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT) {
                    eventRegistry.register(EggNotFound(it))
                    eventRegistry.processEvents()
                }
                match.isPresent
            }
        }
}
