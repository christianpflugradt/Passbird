package de.pflugradts.passbird.application.commandhandling.handler.namespace

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.SwitchNamespaceCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.NamespaceService

class SwitchNamespaceCommandHandler @Inject constructor(
    @Inject private val namespaceService: NamespaceService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleSwitchNamespaceCommand(switchNamespaceCommand: SwitchNamespaceCommand) {
        if (namespaceService.getCurrentNamespace().slot == switchNamespaceCommand.slot) {
            userInterfaceAdapterPort.send(
                outputOf(bytesOf("'${namespaceService.getCurrentNamespace().bytes.asString()}' is already the current namespace.")),
            )
        } else if (namespaceService.atSlot(switchNamespaceCommand.slot).isPresent) {
            namespaceService.updateCurrentNamespace(switchNamespaceCommand.slot)
        } else {
            userInterfaceAdapterPort.send(outputOf(bytesOf("Specified namespace does not exist - Operation aborted.")))
        }
        userInterfaceAdapterPort.sendLineBreak()
    }
}
