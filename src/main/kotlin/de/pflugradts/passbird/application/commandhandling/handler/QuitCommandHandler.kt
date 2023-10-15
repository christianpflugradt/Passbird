package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.commandhandling.command.QuitCommand
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf

class QuitCommandHandler @Inject constructor(
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    @Inject private val bootable: Bootable,
) : CommandHandler {
    @Subscribe
    private fun handleQuitCommand(quitCommand: QuitCommand) {
        userInterfaceAdapterPort.send(outputOf(bytesOf("goodbye")))
        bootable.terminate(SystemOperation())
    }
}
