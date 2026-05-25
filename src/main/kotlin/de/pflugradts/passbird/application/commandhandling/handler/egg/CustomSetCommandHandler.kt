package de.pflugradts.passbird.application.commandhandling.handler.egg

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.CustomSetCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.domain.model.egg.InvalidEggIdException
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction.DO_NOTHING
import jakarta.inject.Inject

class CustomSetCommandHandler @Inject constructor(
    private val configuration: ReadableConfiguration,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleCustomSetCommand(customSetCommand: CustomSetCommand) {
        if (commandConfirmed(customSetCommand)) {
            try {
                passwordService.challengeEggId(customSetCommand.argument)
                val secureInput = userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf("Enter custom Password: ")))
                if (secureInput.isEmpty) {
                    CommandExecutionTracker.markAborted()
                    userInterfaceAdapterPort.send(outputOf(shellOf("Empty input - Operation aborted."), OPERATION_ABORTED))
                } else {
                    if (passwordService.putEgg(customSetCommand.argument, secureInput.shell).failure) {
                        CommandExecutionTracker.markFailure()
                    }
                }
                secureInput.invalidate()
            } catch (ex: InvalidEggIdException) {
                CommandExecutionTracker.markAborted()
                userInterfaceAdapterPort.send(outputOf(shellOf("${ex.message} - Operation aborted."), OPERATION_ABORTED))
            }
        } else {
            CommandExecutionTracker.markAborted()
            userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
        }
        customSetCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun commandConfirmed(customSetCommand: CustomSetCommand) =
        if (configuration.application.password.promptOnRemoval && passwordService.eggExists(customSetCommand.argument, DO_NOTHING)) {
            userInterfaceAdapterPort
                .receiveConfirmation(
                    outputOf(
                        shellOf(
                            "Existing Egg '${customSetCommand.argument.asString()}' will be irrevocably overwritten.\n" +
                                "Input 'c' to confirm or anything else to abort.\nYour input: ",
                        ),
                    ),
                )
        } else {
            true
        }
}
