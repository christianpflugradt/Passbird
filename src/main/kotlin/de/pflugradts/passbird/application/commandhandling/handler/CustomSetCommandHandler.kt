package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.CustomSetCommand
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.domain.model.password.InvalidKeyException
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EntryNotExistsAction.DO_NOTHING

class CustomSetCommandHandler @Inject constructor(
    @Inject private val configuration: ReadableConfiguration,
    @Inject private val passwordService: PasswordService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleCustomSetCommand(customSetCommand: CustomSetCommand) {
        if (commandConfirmed(customSetCommand)) {
            try {
                passwordService.challengeAlias(customSetCommand.argument)
                val secureInput = userInterfaceAdapterPort.receiveSecurely(outputOf(bytesOf("Enter custom password: ")))
                if (secureInput.isEmpty) {
                    userInterfaceAdapterPort.send(outputOf(bytesOf("Empty input - Operation aborted.")))
                } else {
                    passwordService.putPasswordEntry(customSetCommand.argument, secureInput.bytes)
                }
                secureInput.invalidate()
            } catch (ex: InvalidKeyException) {
                val errorMessage = "Password alias cannot contain digits or special characters. Please choose a different alias."
                userInterfaceAdapterPort.send(outputOf(bytesOf(errorMessage)))
            }
        } else {
            userInterfaceAdapterPort.send(outputOf(bytesOf("Operation aborted.")))
        }
        customSetCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun commandConfirmed(customSetCommand: CustomSetCommand) =
        if (configuration.application.password.promptOnRemoval && passwordService.entryExists(customSetCommand.argument, DO_NOTHING)) {
            userInterfaceAdapterPort
                .receiveConfirmation(
                    outputOf(
                        bytesOf(
                            "Existing Password Entry '${customSetCommand.argument.asString()}' will be irrevocably overwritten.\n" +
                                "Input 'c' to confirm or anything else to abort.\nYour input: ",
                        ),
                    ),
                )
        } else {
            true
        }
}
