package de.pflugradts.passbird.application.commandhandling.handler.namespace

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.AddNamespaceCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.DEFAULT
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.NamespaceService

class AddNamespaceCommandHandler @Inject constructor(
    @Inject private val namespaceService: NamespaceService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleAddNamespaceCommand(addNamespaceCommand: AddNamespaceCommand) {
        if (addNamespaceCommand.slot == DEFAULT) {
            userInterfaceAdapterPort.send(outputOf(bytesOf("Default namespace cannot be replaced - Operation aborted.")))
            return
        }
        val prompt = if (namespaceService.atSlot(addNamespaceCommand.slot).isPresent) {
            "Enter new name for existing namespace '${namespaceService.atSlot(addNamespaceCommand.slot).get().bytes.asString()}' " +
                "or nothing to abort%nYour input: "
        } else {
            "Enter name for namespace or nothing to abort\nYour input: "
        }
        val input = userInterfaceAdapterPort.receive(outputOf(bytesOf(prompt)))
        if (input.isEmpty) {
            userInterfaceAdapterPort.send(outputOf(bytesOf("Empty input - Operation aborted.")))
        } else {
            namespaceService.deploy(input.bytes, addNamespaceCommand.slot)
        }
        input.invalidate()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
