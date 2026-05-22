package de.pflugradts.passbird.domain.service.password

import com.google.inject.Inject
import de.pflugradts.passbird.domain.model.egg.EggIdAlreadyExistsException
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.EggRepository

class RenamePasswordService @Inject constructor(
    cryptoProvider: CryptoProvider,
    eggRepository: EggRepository,
    eventRegistry: EventRegistry,
) : CommonPasswordServiceCapabilities(cryptoProvider, eggRepository, eventRegistry) {
    fun renameEgg(eggIdShell: Shell, newEggIdShell: Shell) {
        challengeEggId(newEggIdShell)
        if (eggExists(eggIdShell, CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            if (find(newEggIdShell).isEmpty) {
                encrypted(newEggIdShell).let { find(eggIdShell).get().rename(it) }
                processEventsAndSync()
            } else {
                throw EggIdAlreadyExistsException(newEggIdShell)
            }
        }
    }
}
