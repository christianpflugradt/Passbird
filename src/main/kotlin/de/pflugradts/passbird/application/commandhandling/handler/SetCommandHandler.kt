package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.SetCommand
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.domain.model.egg.InvalidKeyException
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction
import de.pflugradts.passbird.domain.service.password.provider.PasswordProvider

class SetCommandHandler @Inject constructor(
    @Inject private val configuration: ReadableConfiguration,
    @Inject private val passwordService: PasswordService,
    @Inject private val passwordProvider: PasswordProvider,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleSetCommand(setCommand: SetCommand) {
        if (commandConfirmed(setCommand)) {
            try {
                passwordService.challengeAlias(setCommand.argument)
                passwordService.putEgg(
                    setCommand.argument,
                    passwordProvider.createNewPassword(configuration.parsePasswordRequirements()),
                )
            } catch (ex: InvalidKeyException) {
                userInterfaceAdapterPort.send(
                    outputOf(bytesOf("Password alias cannot contain digits or special characters. Please choose a different alias.")),
                )
            }
        } else {
            userInterfaceAdapterPort.send(outputOf(bytesOf("Operation aborted.")))
        }
        setCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun commandConfirmed(setCommand: SetCommand) =
        if (configuration.application.password.promptOnRemoval &&
            passwordService.eggExists(setCommand.argument, EggNotExistsAction.DO_NOTHING)
        ) {
            userInterfaceAdapterPort.receiveConfirmation(
                outputOf(
                    bytesOf(
                        "Existing Password Entry '${setCommand.argument.asString()}' will be irrevocably overwritten.\n" +
                            "Input 'c' to confirm or anything else to abort.\nYour input: ",
                    ),
                ),
            )
        } else {
            true
        }
}
