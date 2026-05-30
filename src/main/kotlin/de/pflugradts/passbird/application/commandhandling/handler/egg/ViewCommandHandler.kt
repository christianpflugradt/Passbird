package de.pflugradts.passbird.application.commandhandling.handler.egg
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.ViewCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.application.useScrambled
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService
class ViewCommandHandler constructor(
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<ViewCommand>(ViewCommand::class.java) {
    override fun handleCommand(command: ViewCommand) {
        passwordService.viewPassword(command.argument).ifPresentOrElse(
            block = { it.useScrambled { shell -> userInterfaceAdapterPort.send(outputOf(shell)) } },
            other = { commandExecutionTracker.markFailure() },
        )
        command.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
