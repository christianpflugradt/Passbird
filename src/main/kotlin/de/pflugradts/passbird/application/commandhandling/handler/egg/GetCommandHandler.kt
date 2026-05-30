package de.pflugradts.passbird.application.commandhandling.handler.egg
import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.GetCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.application.useScrambled
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService
class GetCommandHandler constructor(
    private val passwordService: PasswordService,
    private val clipboardAdapterPort: ClipboardAdapterPort,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<GetCommand>(GetCommand::class.java) {
    override fun handleCommand(command: GetCommand) {
        passwordService.viewPassword(command.argument).ifPresentOrElse(
            block = { password ->
                password.useScrambled {
                    val clipboardResult = clipboardAdapterPort.post(outputOf(it))
                    if (clipboardResult.failure) {
                        commandExecutionTracker.markFailure()
                    }
                    clipboardResult.onSuccess {
                        userInterfaceAdapterPort.send(outputOf(shellOf("Password copied to clipboard.")))
                    }
                }
            },
            other = {
                commandExecutionTracker.markFailure()
            },
        )
        command.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
