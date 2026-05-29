package de.pflugradts.passbird.application.commandhandling.handler.egg

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.ViewCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.application.useScrambled
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService
import jakarta.inject.Inject

class ViewCommandHandler @Inject constructor(
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : CommandHandler {
    @Subscribe
    private fun handleViewCommand(viewCommand: ViewCommand) {
        passwordService.viewPassword(viewCommand.argument).ifPresentOrElse(
            block = { it.useScrambled { shell -> userInterfaceAdapterPort.send(outputOf(shell)) } },
            other = { commandExecutionTracker.markFailure() },
        )
        viewCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
