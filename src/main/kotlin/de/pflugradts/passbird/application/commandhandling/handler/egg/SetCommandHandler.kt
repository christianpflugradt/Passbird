package de.pflugradts.passbird.application.commandhandling.handler.egg
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.SetCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
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
class SetCommandHandler constructor(
    private val configuration: ReadableConfiguration,
    private val passwordService: PasswordService,
    private val passwordProvider: PasswordProvider,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<SetCommand>(SetCommand::class.java) {
    private val customPasswordConfigurations: List<ReadableConfiguration.CustomPasswordConfiguration>
        get() = configuration.application.password.customPasswordConfigurations
    override fun handleCommand(command: SetCommand) {
        if (command.slot != DEFAULT && customPasswordConfigurations.size < command.slot.index()) {
            commandExecutionTracker.markAborted()
            val msg = "Specified configuration does not exist - Operation aborted."
            userInterfaceAdapterPort.send(outputOf(shellOf(msg), OPERATION_ABORTED))
        } else {
            val passwordRequirements = if (command.slot == DEFAULT) {
                configuration.parsePasswordRequirements()
            } else {
                customPasswordConfigurations[command.slot.index() - 1].toPasswordRequirements()
            }
            if (!passwordRequirements.isValid()) {
                commandExecutionTracker.markAborted()
                val msg = "Specified configuration is invalid - Operation aborted."
                userInterfaceAdapterPort.send(outputOf(shellOf(msg), OPERATION_ABORTED))
            } else if (commandConfirmed(command)) {
                try {
                    passwordService.challengeEggId(command.argument)
                    if (passwordService.putEgg(command.argument, passwordProvider.createNewPassword(passwordRequirements)).failure) {
                        commandExecutionTracker.markFailure()
                    }
                } catch (ex: InvalidEggIdException) {
                    commandExecutionTracker.markAborted()
                    userInterfaceAdapterPort.send(outputOf(shellOf("${ex.message} - Operation aborted."), OPERATION_ABORTED))
                }
            } else {
                commandExecutionTracker.markAborted()
                userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
            }
        }
        command.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
    private fun commandConfirmed(command: SetCommand) = if (configuration.application.password.promptOnRemoval &&
        passwordService.eggExists(command.argument, EggNotExistsAction.DO_NOTHING)
    ) {
        val msg = "Existing Egg '${command.argument.asString()}' will be irrevocably overwritten.\n" +
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
