package de.pflugradts.passbird.application.commandhandling.handler.namespace

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.AssignNamespaceCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.NamespaceService
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EntryNotExistsAction

class AssignNamespaceCommandHandler @Inject constructor(
    @Inject private val namespaceService: NamespaceService,
    @Inject private val passwordService: PasswordService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler, CanListAvailableNamespaces(namespaceService) {
    @Subscribe
    private fun handleAssignNamespaceCommand(assignNamespaceCommand: AssignNamespaceCommand) {
        if (passwordService.entryExists(assignNamespaceCommand.argument, EntryNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            userInterfaceAdapterPort.send(
                outputOf(bytesOf("Available namespaces: \n${getAvailableNamespaces(includeCurrent = false)}")),
            )
            val input = userInterfaceAdapterPort.receive(outputOf(bytesOf("Enter namespace you want to move password entry to: ")))
            val namespace = input.parseNamespace()
            if (namespace === NamespaceSlot.INVALID) {
                userInterfaceAdapterPort.send(outputOf(bytesOf("Invalid namespace - Operation aborted.")))
            } else if (namespace === namespaceService.getCurrentNamespace().slot) {
                userInterfaceAdapterPort.send(
                    outputOf(bytesOf("Password entry is already in the specified namespace - Operation aborted.")),
                )
            } else if (namespaceService.atSlot(namespace).isEmpty) {
                userInterfaceAdapterPort.send(outputOf(bytesOf("Specified namespace does not exist - Operation aborted.")))
            } else if (passwordService.entryExists(assignNamespaceCommand.argument, namespace)) {
                userInterfaceAdapterPort.send(
                    outputOf(bytesOf("Password entry with same alias already exists in target namespace - Operation aborted.")),
                )
            } else {
                passwordService.movePasswordEntry(assignNamespaceCommand.argument, namespace)
            }
            input.invalidate()
        }
        assignNamespaceCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
