package de.pflugradts.passbird.application.commandhandling.handler.yolk

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.DiscardYolkCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT

class DiscardYolkCommandHandler(
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<DiscardYolkCommand>(DiscardYolkCommand::class.java) {
    override fun handleCommand(command: DiscardYolkCommand) {
        if (!passwordService.eggExists(command.argument, CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            commandExecutionTracker.markFailure()
        } else if (passwordService.viewYolk(command.argument).isEmpty) {
            commandExecutionTracker.markAborted()
            userInterfaceAdapterPort.send(outputOf(shellOf("Yolk not found - Operation aborted."), OPERATION_ABORTED))
        } else if (
            !userInterfaceAdapterPort.receiveConfirmation(
                outputOf(
                    shellOf(
                        "Discarding a Yolk is an irrevocable action.\n" +
                            "Input 'c' to confirm or anything else to abort.\nYour input: ",
                    ),
                ),
            )
        ) {
            commandExecutionTracker.markAborted()
            userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
        } else if (passwordService.discardYolk(command.argument).failure) {
            commandExecutionTracker.markFailure()
        }
        command.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
