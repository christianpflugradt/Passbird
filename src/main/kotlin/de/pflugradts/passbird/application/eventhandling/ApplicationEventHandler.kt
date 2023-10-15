package de.pflugradts.passbird.application.eventhandling

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.event.PasswordEntryCreated
import de.pflugradts.passbird.domain.model.event.PasswordEntryDiscarded
import de.pflugradts.passbird.domain.model.event.PasswordEntryNotFound
import de.pflugradts.passbird.domain.model.event.PasswordEntryRenamed
import de.pflugradts.passbird.domain.model.event.PasswordEntryUpdated
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
    private fun handlePasswordEntryCreated(passwordEntryCreated: PasswordEntryCreated) {
        send("PasswordEntry '${decrypt(passwordEntryCreated.passwordEntry.viewKey())}' successfully created.")
    }

    @Subscribe
    private fun handlePasswordEntryUpdated(passwordEntryUpdated: PasswordEntryUpdated) {
        send("PasswordEntry '${decrypt(passwordEntryUpdated.passwordEntry.viewKey())}' successfully updated.")
    }

    @Subscribe
    private fun handlePasswordEntryRenamed(passwordEntryRenamed: PasswordEntryRenamed) {
        send("PasswordEntry '${decrypt(passwordEntryRenamed.passwordEntry.viewKey())}' successfully renamed.")
    }

    @Subscribe
    private fun handlePasswordEntryDiscarded(passwordEntryDiscarded: PasswordEntryDiscarded) {
        send("PasswordEntry '${decrypt(passwordEntryDiscarded.passwordEntry.viewKey())}' successfully deleted.")
    }

    @Subscribe
    private fun handlePasswordEntryNotFound(passwordEntryNotFound: PasswordEntryNotFound) {
        send("PasswordEntry '${decrypt(passwordEntryNotFound.keyBytes)}' not found.")
    }

    private fun send(str: String) = userInterfaceAdapterPort.send(outputOf(bytesOf(str)))
    private fun decrypt(bytes: Bytes) = cryptoProvider.decrypt(bytes).asString()
}
