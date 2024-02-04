package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.SetCommand
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.domain.model.egg.InvalidEggIdException
import de.pflugradts.passbird.domain.model.egg.PasswordRequirements
import de.pflugradts.passbird.domain.model.nest.NestSlot.DEFAULT
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction
import de.pflugradts.passbird.domain.service.password.provider.PasswordProvider

class SetCommandHandler @Inject constructor(
    @Inject private val configuration: ReadableConfiguration,
    @Inject private val passwordService: PasswordService,
    @Inject private val passwordProvider: PasswordProvider,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
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
                customPasswordConfigurations[setCommand.slot.index() - 1].let {
                    PasswordRequirements(
                        length = it.length,
                        hasNumbers = it.hasNumbers,
                        hasLowercaseLetters = it.hasLowercaseLetters,
                        hasUppercaseLetters = it.hasUppercaseLetters,
                        hasSpecialCharacters = it.hasSpecialCharacters,
                        unusedSpecialCharacters = it.unusedSpecialCharacters,
                    )
                }
            }
            if (commandConfirmed(setCommand)) {
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

    private fun commandConfirmed(setCommand: SetCommand) =
        if (configuration.application.password.promptOnRemoval &&
            passwordService.eggExists(setCommand.argument, EggNotExistsAction.DO_NOTHING)
        ) {
            val msg = "Existing Egg '${setCommand.argument.asString()}' will be irrevocably overwritten.\n" +
                "Input 'c' to confirm or anything else to abort.\nYour input: "
            userInterfaceAdapterPort.receiveConfirmation(outputOf(shellOf(msg)))
        } else {
            true
        }
}
