package de.pflugradts.passbird.application.eventhandling

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.event.EggCreated
import de.pflugradts.passbird.domain.model.event.EggDiscarded
import de.pflugradts.passbird.domain.model.event.EggMoved
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.event.EggRenamed
import de.pflugradts.passbird.domain.model.event.EggUpdated
import de.pflugradts.passbird.domain.model.event.EggsExported
import de.pflugradts.passbird.domain.model.event.EggsImported
import de.pflugradts.passbird.domain.model.event.NestCreated
import de.pflugradts.passbird.domain.model.event.NestDiscarded
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.EVENT_HANDLED
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
        send("Egg '${decrypt(eggDiscarded.egg.viewEggId())}' successfully discarded.")
    }

    @Subscribe
    private fun handleEggMoved(eggMoved: EggMoved) {
        send("Egg '${decrypt(eggMoved.egg.viewEggId())}' successfully moved to ${nestSlotText(eggMoved.egg.associatedNest().index())}.")
    }

    @Subscribe
    private fun handleEggNotFound(eggNotFound: EggNotFound) {
        send("Egg '${decrypt(eggNotFound.eggIdShell)}' not found.")
        userInterfaceAdapterPort.warningSound()
    }

    @Subscribe
    private fun handleEggsExported(eggsExported: EggsExported) {
        send("${eggsExported.count} eggs successfully exported.")
    }

    @Subscribe
    private fun handleEggsImported(eggsImported: EggsImported) {
        send("${eggsImported.count} eggs successfully imported.")
    }

    @Subscribe
    private fun handleNestCreated(nestCreated: NestCreated) {
        send("Nest '${nestCreated.nest.viewNestId().asString()}' successfully created.")
    }

    @Subscribe
    private fun handleNestDiscarded(nestDiscarded: NestDiscarded) {
        send("Nest '${nestDiscarded.nest.viewNestId().asString()}' successfully discarded.")
    }

    private fun send(str: String) = userInterfaceAdapterPort.send(outputOf(shellOf(str), EVENT_HANDLED))
    private fun decrypt(shell: Shell) = cryptoProvider.decrypt(shell).asString()
}

private fun nestSlotText(nestSlotIndex: Int) = if (nestSlotIndex in 1..9) "Nest at Slot $nestSlotIndex" else "Default Nest"
