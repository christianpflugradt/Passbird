package de.pflugradts.passbird.application.commandhandling.handler.egg

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.SetCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.domain.model.egg.InvalidEggIdException
import de.pflugradts.passbird.domain.model.egg.PasswordRequirements
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction
import de.pflugradts.passbird.domain.service.password.provider.PasswordProvider

class SetCommandHandler @Inject constructor(
    private val configuration: ReadableConfiguration,
    private val passwordService: PasswordService,
    private val passwordProvider: PasswordProvider,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {

    private val customPasswordConfigurations: List<ReadableConfiguration.CustomPasswordConfiguration>
        get() = configuration.application.password.customPasswordConfigurations

    @Subscribe
    private fun handleSetCommand(setCommand: SetCommand) {
        if (setCommand.slot != DEFAULT && customPasswordConfigurations.size < setCommand.slot.index()) {
            val msg = "Specified configuration does not exist - Operation aborted."
            userInterfaceAdapterPort.send(outputOf(shellOf(msg), OPERATION_ABORTED))
        } else {
            val passwordRequirements = if (setCommand.slot == DEFAULT) {
                configuration.parsePasswordRequirements()
            } else {
                customPasswordConfigurations[setCommand.slot.index() - 1].toPasswordRequirements()
            }
            if (!passwordRequirements.isValid()) {
                val msg = "Specified configuration is invalid - Operation aborted."
                userInterfaceAdapterPort.send(outputOf(shellOf(msg), OPERATION_ABORTED))
            } else if (commandConfirmed(setCommand)) {
                try {
                    passwordService.challengeEggId(setCommand.argument)
                    passwordService.putEgg(setCommand.argument, passwordProvider.createNewPassword(passwordRequirements))
                } catch (ex: InvalidEggIdException) {
                    userInterfaceAdapterPort.send(outputOf(shellOf("${ex.message} - Operation aborted."), OPERATION_ABORTED))
                }
            } else {
                userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
            }
        }
        setCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun commandConfirmed(setCommand: SetCommand) = if (configuration.application.password.promptOnRemoval &&
        passwordService.eggExists(setCommand.argument, EggNotExistsAction.DO_NOTHING)
    ) {
        val msg = "Existing Egg '${setCommand.argument.asString()}' will be irrevocably overwritten.\n" +
            "Input 'c' to confirm or anything else to abort.\nYour input: "
        userInterfaceAdapterPort.receiveConfirmation(outputOf(shellOf(msg)))
    } else {
        true
    }
}

private fun ReadableConfiguration.CustomPasswordConfiguration.toPasswordRequirements() = PasswordRequirements(
    length = length,
    hasNumbers = hasNumbers,
    hasLowercaseLetters = hasLowercaseLetters,
    hasUppercaseLetters = hasUppercaseLetters,
    hasSpecialCharacters = hasSpecialCharacters,
    unusedSpecialCharacters = unusedSpecialCharacters,
)
