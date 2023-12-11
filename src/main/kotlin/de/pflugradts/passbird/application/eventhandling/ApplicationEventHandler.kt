package de.pflugradts.passbird.application.eventhandling

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.event.EggCreated
import de.pflugradts.passbird.domain.model.event.EggDiscarded
import de.pflugradts.passbird.domain.model.event.EggNotFound
import de.pflugradts.passbird.domain.model.event.EggRenamed
import de.pflugradts.passbird.domain.model.event.EggUpdated
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.eventhandling.EventHandler
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider

class ApplicationEventHandler @Inject constructor(
    @Inject private val cryptoProvider: CryptoProvider,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : EventHandler {
    @Subscribe
    private fun handleEggCreated(eggCreated: EggCreated) {
        send("PasswordEntry '${decrypt(eggCreated.egg.viewKey())}' successfully created.")
    }

    @Subscribe
    private fun handleEggUpdated(eggUpdated: EggUpdated) {
        send("PasswordEntry '${decrypt(eggUpdated.egg.viewKey())}' successfully updated.")
    }

    @Subscribe
    private fun handleEggRenamed(eggRenamed: EggRenamed) {
        send("PasswordEntry '${decrypt(eggRenamed.egg.viewKey())}' successfully renamed.")
    }

    @Subscribe
    private fun handleEggDiscarded(eggDiscarded: EggDiscarded) {
        send("PasswordEntry '${decrypt(eggDiscarded.egg.viewKey())}' successfully deleted.")
    }

    @Subscribe
    private fun handleEggNotFound(eggNotFound: EggNotFound) {
        send("PasswordEntry '${decrypt(eggNotFound.keyBytes)}' not found.")
    }

    private fun send(str: String) = userInterfaceAdapterPort.send(outputOf(bytesOf(str)))
    private fun decrypt(bytes: Bytes) = cryptoProvider.decrypt(bytes).asString()
}
