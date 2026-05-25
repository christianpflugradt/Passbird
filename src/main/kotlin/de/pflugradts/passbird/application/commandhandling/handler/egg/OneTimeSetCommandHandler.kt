package de.pflugradts.passbird.application.commandhandling.handler.egg

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.OneTimeSetCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.domain.model.egg.InvalidEggIdException
import de.pflugradts.passbird.domain.model.egg.PasswordRequirements
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction.DO_NOTHING
import de.pflugradts.passbird.domain.service.password.provider.PasswordProvider
import jakarta.inject.Inject

class OneTimeSetCommandHandler @Inject constructor(
    private val configuration: ReadableConfiguration,
    private val passwordService: PasswordService,
    private val passwordProvider: PasswordProvider,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleOneTimeSetCommand(oneTimeSetCommand: OneTimeSetCommand) {
        try {
            passwordService.challengeEggId(oneTimeSetCommand.argument)
            val passwordLengthInput = receivePasswordLength()
            when {
                passwordLengthInput.isAborted -> {
                    CommandExecutionTracker.markAborted()
                    userInterfaceAdapterPort.send(outputOf(shellOf("Empty input - Operation aborted."), OPERATION_ABORTED))
                }

                passwordLengthInput.value == null -> {
                    CommandExecutionTracker.markAborted()
                    userInterfaceAdapterPort.send(
                        outputOf(shellOf("Specified configuration is invalid - Operation aborted."), OPERATION_ABORTED),
                    )
                }

                else -> {
                    val passwordRequirements = receivePasswordRequirements(passwordLengthInput.value)
                    when {
                        !passwordRequirements.isValid() -> {
                            CommandExecutionTracker.markAborted()
                            userInterfaceAdapterPort.send(
                                outputOf(shellOf("Specified configuration is invalid - Operation aborted."), OPERATION_ABORTED),
                            )
                        }

                        commandConfirmed(oneTimeSetCommand) -> {
                            if (passwordService.putEgg(
                                    oneTimeSetCommand.argument,
                                    passwordProvider.createNewPassword(passwordRequirements),
                                ).failure
                            ) {
                                CommandExecutionTracker.markFailure()
                            }
                        }

                        else -> {
                            CommandExecutionTracker.markAborted()
                            userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
                        }
                    }
                }
            }
        } catch (ex: InvalidEggIdException) {
            CommandExecutionTracker.markAborted()
            userInterfaceAdapterPort.send(outputOf(shellOf("${ex.message} - Operation aborted."), OPERATION_ABORTED))
        }
        oneTimeSetCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun receivePasswordLength() = userInterfaceAdapterPort.receive(
        outputOf(shellOf("Enter password length or just press enter to abort: ")),
    ).let {
        try {
            PasswordLengthInput(
                value = if (it.isEmpty) null else it.shell.asString().toIntOrNull(),
                isAborted = it.isEmpty,
            )
        } finally {
            it.invalidate()
        }
    }

    private fun receivePasswordRequirements(passwordLength: Int): PasswordRequirements {
        val hasNumbers = userInterfaceAdapterPort.receiveYes(outputOf(shellOf("Include numbers? Y/n ")))
        val hasLowercaseLetters = userInterfaceAdapterPort.receiveYes(outputOf(shellOf("Include lowercase letters? Y/n ")))
        val hasUppercaseLetters = userInterfaceAdapterPort.receiveYes(outputOf(shellOf("Include uppercase letters? Y/n ")))
        val hasSpecialCharacters = userInterfaceAdapterPort.receiveYes(outputOf(shellOf("Include special characters? Y/n ")))
        return PasswordRequirements(
            length = passwordLength,
            hasNumbers = hasNumbers,
            hasLowercaseLetters = hasLowercaseLetters,
            hasUppercaseLetters = hasUppercaseLetters,
            hasSpecialCharacters = hasSpecialCharacters,
            unusedSpecialCharacters = if (hasSpecialCharacters) receiveUnusedSpecialCharacters() else "",
        )
    }

    private fun receiveUnusedSpecialCharacters() = userInterfaceAdapterPort.receive(
        outputOf(shellOf("Enter unused special characters or just press enter to keep all: ")),
    ).let {
        try {
            it.shell.asString()
        } finally {
            it.invalidate()
        }
    }

    private fun commandConfirmed(oneTimeSetCommand: OneTimeSetCommand) =
        if (configuration.application.password.promptOnRemoval && passwordService.eggExists(oneTimeSetCommand.argument, DO_NOTHING)) {
            userInterfaceAdapterPort
                .receiveConfirmation(
                    outputOf(
                        shellOf(
                            "Existing Egg '${oneTimeSetCommand.argument.asString()}' will be irrevocably overwritten.\n" +
                                "Input 'c' to confirm or anything else to abort.\nYour input: ",
                        ),
                    ),
                )
        } else {
            true
        }
}

private data class PasswordLengthInput(val value: Int?, val isAborted: Boolean)
