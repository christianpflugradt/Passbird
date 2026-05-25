package de.pflugradts.passbird.application.commandhandling.handler.egg

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.ViewCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService
import jakarta.inject.Inject

class ViewCommandHandler @Inject constructor(
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleViewCommand(viewCommand: ViewCommand) {
        passwordService.viewPassword(viewCommand.argument).ifPresentOrElse(
            block = { userInterfaceAdapterPort.send(outputOf(it)) },
            other = { CommandExecutionTracker.markFailure() },
        )
        viewCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
