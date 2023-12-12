package de.pflugradts.passbird.application.commandhandling.handler.nest

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.AssignNestCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.NestService
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction

class AssignNestCommandHandler @Inject constructor(
    @Inject private val nestService: NestService,
    @Inject private val passwordService: PasswordService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler, CanListAvailableNests(nestService) {
    @Subscribe
    private fun handleAssignNestCommand(assignNestCommand: AssignNestCommand) {
        if (passwordService.eggExists(assignNestCommand.argument, EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            userInterfaceAdapterPort.send(
                outputOf(shellOf("Available namespaces: \n${getAvailableNests(includeCurrent = false)}")),
            )
            val input = userInterfaceAdapterPort.receive(outputOf(shellOf("Enter namespace you want to move password entry to: ")))
            val nestSlot = input.extractNestSlot()
            if (nestSlot === NestSlot.INVALID) {
                userInterfaceAdapterPort.send(outputOf(shellOf("Invalid namespace - Operation aborted.")))
            } else if (nestSlot === nestService.currentNest().nestSlot) {
                userInterfaceAdapterPort.send(
                    outputOf(shellOf("Password entry is already in the specified namespace - Operation aborted.")),
                )
            } else if (nestService.atNestSlot(nestSlot).isEmpty) {
                userInterfaceAdapterPort.send(outputOf(shellOf("Specified namespace does not exist - Operation aborted.")))
            } else if (passwordService.eggExists(assignNestCommand.argument, nestSlot)) {
                userInterfaceAdapterPort.send(
                    outputOf(shellOf("Password entry with same alias already exists in target namespace - Operation aborted.")),
                )
            } else {
                passwordService.moveEgg(assignNestCommand.argument, nestSlot)
            }
            input.invalidate()
        }
        assignNestCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
