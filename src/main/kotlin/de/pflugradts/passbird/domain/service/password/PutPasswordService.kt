package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.EggRepository
import java.util.stream.Stream

class PutPasswordService @Inject constructor(
    cryptoProvider: CryptoProvider,
    private val eggRepository: EggRepository,
    eventRegistry: EventRegistry,
    private val nestService: NestService,
) : CommonPasswordServiceCapabilities(cryptoProvider, eggRepository, eventRegistry) {
    fun putEggs(shellPairs: Stream<ShellPair>) {
        shellPairs.forEach { putEgg(it.first, it.second, false) }
        processEventsAndSync()
    }

    fun putEgg(eggIdShell: Shell, passwordShell: Shell, sync: Boolean = true) {
        challengeEggId(eggIdShell)
        val encryptedEggIdShell = encrypted(eggIdShell)
        val encryptedPasswordShell = encrypted(passwordShell)
        val nestSlot = nestService.currentNest().slot
        find(encryptedEggIdShell).ifPresentOrElse(
            { it.updatePassword(encryptedPasswordShell) },
            { eggRepository.add(createEgg(nestSlot, encryptedEggIdShell, encryptedPasswordShell)) },
        )
        if (sync) processEventsAndSync()
    }

    fun putProtein(eggIdShell: Shell, slot: Slot, typeShell: Shell, structureShell: Shell) {
        if (eggExists(eggIdShell, CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            find(encrypted(eggIdShell)).get().updateProtein(slot, encrypted(typeShell), encrypted(structureShell))
        }
        processEventsAndSync()
    }
}
