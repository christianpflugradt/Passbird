package de.pflugradts.passbird.application.commandhandling.handler.nest

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.AssignNestCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.NestService
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EntryNotExistsAction

class AssignNestCommandHandler @Inject constructor(
    @Inject private val nestService: NestService,
    @Inject private val passwordService: PasswordService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler, CanListAvailableNests(nestService) {
    @Subscribe
    private fun handleAssignNestCommand(assignNestCommand: AssignNestCommand) {
        if (passwordService.entryExists(assignNestCommand.argument, EntryNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            userInterfaceAdapterPort.send(
                outputOf(bytesOf("Available namespaces: \n${getAvailableNests(includeCurrent = false)}")),
            )
            val input = userInterfaceAdapterPort.receive(outputOf(bytesOf("Enter namespace you want to move password entry to: ")))
            val nestSlot = input.extractNestSlot()
            if (nestSlot === Slot.INVALID) {
                userInterfaceAdapterPort.send(outputOf(bytesOf("Invalid namespace - Operation aborted.")))
            } else if (nestSlot === nestService.getCurrentNest().slot) {
                userInterfaceAdapterPort.send(
                    outputOf(bytesOf("Password entry is already in the specified namespace - Operation aborted.")),
                )
            } else if (nestService.atSlot(nestSlot).isEmpty) {
                userInterfaceAdapterPort.send(outputOf(bytesOf("Specified namespace does not exist - Operation aborted.")))
            } else if (passwordService.entryExists(assignNestCommand.argument, nestSlot)) {
                userInterfaceAdapterPort.send(
                    outputOf(bytesOf("Password entry with same alias already exists in target namespace - Operation aborted.")),
                )
            } else {
                passwordService.movePasswordEntry(assignNestCommand.argument, nestSlot)
            }
            input.invalidate()
        }
        assignNestCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
