package de.pflugradts.passbird.application.commandhandling.handler.egg

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.DiscardCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT

class DiscardCommandHandler constructor(
    private val configuration: ReadableConfiguration,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<DiscardCommand>(DiscardCommand::class.java) {
    override fun handleCommand(command: DiscardCommand) {
        if (!passwordService.eggExists(command.argument, CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            command.invalidateInput()
            userInterfaceAdapterPort.sendLineBreak()
            return
        }
        val action = if (command.force) {
            { passwordService.discardEggPermanently(command.argument) }
        } else {
            { passwordService.discardEgg(command.argument) }
        }
        if (commandConfirmed(command.force)) {
            if (action().failure) {
                commandExecutionTracker.markFailure()
            }
        } else {
            commandExecutionTracker.markAborted()
            userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
        }
        command.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun commandConfirmed(force: Boolean) = if (configuration.application.password.promptOnRemoval) {
        userInterfaceAdapterPort.receiveConfirmation(
            outputOf(
                shellOf(
                    if (force) {
                        """
                        Discarding an Egg is an irrevocable action.
                        Input 'c' to confirm or anything else to abort.
                        Your input: 
                        """.trimIndent()
                    } else {
                        """
                        Discarding an Egg will move it to trash.
                        Input 'c' to confirm or anything else to abort.
                        Your input: 
                        """.trimIndent()
                    },
                ),
            ),
        )
    } else {
        true
    }
}
