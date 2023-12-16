package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.CustomSetCommand
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.domain.model.egg.InvalidEggIdException
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction.DO_NOTHING

class CustomSetCommandHandler @Inject constructor(
    @Inject private val configuration: ReadableConfiguration,
    @Inject private val passwordService: PasswordService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleCustomSetCommand(customSetCommand: CustomSetCommand) {
        if (commandConfirmed(customSetCommand)) {
            try {
                passwordService.challengeEggId(customSetCommand.argument)
                val secureInput = userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf("Enter custom Password: ")))
                if (secureInput.isEmpty) {
                    userInterfaceAdapterPort.send(outputOf(shellOf("Empty input - Operation aborted.")))
                } else {
                    passwordService.putEgg(customSetCommand.argument, secureInput.shell)
                }
                secureInput.invalidate()
            } catch (ex: InvalidEggIdException) {
                val errorMessage = "EggId cannot contain digits or special characters. Please choose a different EggId."
                userInterfaceAdapterPort.send(outputOf(shellOf(errorMessage)))
            }
        } else {
            userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted.")))
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
