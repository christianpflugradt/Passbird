package de.pflugradts.passbird.application.commandhandling.handler.nest

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.SwitchNestCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.NestService

class SwitchNestCommandHandler @Inject constructor(
    @Inject private val nestService: NestService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleSwitchNestCommand(switchNestCommand: SwitchNestCommand) {
        if (nestService.getCurrentNest().slot == switchNestCommand.slot) {
            userInterfaceAdapterPort.send(
                outputOf(shellOf("'${nestService.getCurrentNest().shell.asString()}' is already the current namespace.")),
            )
        } else if (nestService.atSlot(switchNestCommand.slot).isPresent) {
            nestService.moveToNestAt(switchNestCommand.slot)
        } else {
            userInterfaceAdapterPort.send(outputOf(shellOf("Specified namespace does not exist - Operation aborted.")))
        }
        userInterfaceAdapterPort.sendLineBreak()
    }
}
