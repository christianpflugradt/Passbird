package de.pflugradts.passbird.application.commandhandling.handler.yolk

import de.pflugradts.passbird.application.SecureInputUnavailableException
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.SetYolkCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.yolk.ConfiguredYolkDefaults
import de.pflugradts.passbird.application.yolk.YolkInputParser
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT

class SetYolkCommandHandler(
    private val configuration: ReadableConfiguration,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<SetYolkCommand>(SetYolkCommand::class.java) {
    override fun handleCommand(command: SetYolkCommand) {
        try {
            if (!eggExists(command) || !overwriteConfirmed(command)) return
            receiveYolkInput()?.let { input -> processInput(command, input) }
        } finally {
            command.invalidateInput()
            userInterfaceAdapterPort.sendLineBreak()
        }
    }

    private val configuredDefaults get() = ConfiguredYolkDefaults(
        algorithm = configuration.application.yolk.algorithm,
        digits = configuration.application.yolk.digits,
        periodSeconds = configuration.application.yolk.periodSeconds,
    )

    private fun eggExists(command: SetYolkCommand) = passwordService.eggExists(command.argument, CREATE_ENTRY_NOT_EXISTS_EVENT).also {
        if (!it) commandExecutionTracker.markFailure()
    }

    private fun overwriteConfirmed(command: SetYolkCommand): Boolean {
        val existingYolk = passwordService.viewYolk(command.argument)
        return !existingYolk.isPresent || userInterfaceAdapterPort.receiveConfirmation(
            outputOf(
                shellOf(
                    "Existing Yolk of Egg '${command.argument.asString()}' will be irrevocably overwritten.\n" +
                        "Input 'c' to confirm or anything else to abort.\nYour input: ",
                ),
            ),
        ).also {
            if (!it) abortOperation()
        }
    }

    private fun receiveYolkInput() = try {
        userInterfaceAdapterPort.receiveSecurely(
            outputOf(shellOf("Enter Yolk secret or otpauth URI or just press enter to abort: ")),
        )
    } catch (_: SecureInputUnavailableException) {
        abortOperation()
        null
    }

    private fun processInput(command: SetYolkCommand, input: de.pflugradts.passbird.domain.model.transfer.Input) {
        try {
            if (input.isEmpty) {
                abortOperation()
                return
            }
            persistYolk(command, input.shell)
        } catch (ex: IllegalArgumentException) {
            commandExecutionTracker.markAborted()
            userInterfaceAdapterPort.send(outputOf(shellOf("${ex.message} - Operation aborted."), OPERATION_ABORTED))
        } finally {
            input.invalidate()
        }
    }

    private fun persistYolk(command: SetYolkCommand, inputShell: de.pflugradts.passbird.domain.model.shell.Shell) {
        val parsed = YolkInputParser().parse(inputShell, configuredDefaults)
        try {
            if (
                passwordService.putYolk(
                    eggIdShell = command.argument,
                    secretShell = parsed.secret,
                    algorithm = parsed.algorithm,
                    digits = parsed.digits,
                    periodSeconds = parsed.periodSeconds,
                ).failure
            ) {
                commandExecutionTracker.markFailure()
            }
        } finally {
            parsed.secret.scramble()
        }
    }

    private fun abortOperation() {
        commandExecutionTracker.markAborted()
        userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
    }
}
