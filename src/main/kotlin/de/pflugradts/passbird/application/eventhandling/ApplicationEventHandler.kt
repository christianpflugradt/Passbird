package de.pflugradts.passbird.application.eventhandling

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.event.EggCreated
import de.pflugradts.passbird.domain.model.event.EggDiscarded
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.event.EggRenamed
import de.pflugradts.passbird.domain.model.event.EggUpdated
import de.pflugradts.passbird.domain.model.event.EggsExported
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.eventhandling.EventHandler
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider

class ApplicationEventHandler @Inject constructor(
    @Inject private val cryptoProvider: CryptoProvider,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : EventHandler {
    @Subscribe
    private fun handleEggCreated(eggCreated: EggCreated) {
        send("Egg '${decrypt(eggCreated.egg.viewEggId())}' successfully created.")
    }

    @Subscribe
    private fun handleEggUpdated(eggUpdated: EggUpdated) {
        send("Egg '${decrypt(eggUpdated.egg.viewEggId())}' successfully updated.")
    }

    @Subscribe
    private fun handleEggRenamed(eggRenamed: EggRenamed) {
        send("Egg '${decrypt(eggRenamed.egg.viewEggId())}' successfully renamed.")
    }

    @Subscribe
    private fun handleEggDiscarded(eggDiscarded: EggDiscarded) {
        send("Egg '${decrypt(eggDiscarded.egg.viewEggId())}' successfully deleted.")
    }

    @Subscribe
    private fun handleEggNotFound(eggNotFound: EggNotFound) {
        send("Egg '${decrypt(eggNotFound.eggIdShell)}' not found.")
    }

    @Subscribe
    private fun handleEggsExported(eggsExported: EggsExported) {
        send("${eggsExported.count} eggs successfully exported.")
    }

    private fun send(str: String) = userInterfaceAdapterPort.send(outputOf(shellOf(str)))
    private fun decrypt(shell: Shell) = cryptoProvider.decrypt(shell).asString()
}
