package de.pflugradts.passbird.application.commandhandling.handler.nest

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.SwitchNestCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.nest.NestService

class SwitchNestCommandHandler @Inject constructor(
    private val nestService: NestService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleSwitchNestCommand(switchNestCommand: SwitchNestCommand) {
        if (nestService.currentNest().slot == switchNestCommand.slot) {
            userInterfaceAdapterPort.send(
                outputOf(shellOf("'${nestService.currentNest().viewNestId().asString()}' is already the current Nest.")),
            )
        } else if (nestService.atNestSlot(switchNestCommand.slot).isPresent) {
            nestService.moveToNestAt(switchNestCommand.slot)
        } else {
            userInterfaceAdapterPort.send(outputOf(shellOf("Specified Nest does not exist - Operation aborted."), OPERATION_ABORTED))
        }
        userInterfaceAdapterPort.sendLineBreak()
    }
}
