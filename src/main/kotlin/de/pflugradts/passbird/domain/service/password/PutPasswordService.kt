package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.service.NestService
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.storage.EggRepository
import java.util.stream.Stream

class PutPasswordService @Inject constructor(
    @Inject private val cryptoProvider: CryptoProvider,
    @Inject private val eggRepository: EggRepository,
    @Inject private val eventRegistry: EventRegistry,
    @Inject private val nestService: NestService,
) : CommonPasswordServiceCapabilities(cryptoProvider, eggRepository, eventRegistry) {
    fun putEggs(shellPairs: Stream<ShellPair>) {
        shellPairs.forEach { putEgg(it.first, it.second, false) }
        processEventsAndSync()
    }

    fun putEgg(eggIdShell: Shell, passwordShell: Shell, sync: Boolean = true) {
        challengeEggId(eggIdShell)
        val encryptedEggIdShell = encrypted(eggIdShell)
        val encryptedPasswordShell = encrypted(passwordShell)
        val nestSlot = nestService.currentNest().nestSlot
        find(encryptedEggIdShell).ifPresentOrElse(
            { it.updatePassword(encryptedPasswordShell) },
            { eggRepository.add(createEgg(nestSlot, encryptedEggIdShell, encryptedPasswordShell)) },
        )
        if (sync) processEventsAndSync()
    }
}
