package de.pflugradts.passbird.application.commandhandling.handler.protein

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.SecureInputUnavailableException
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.SetProteinCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT
import jakarta.inject.Inject

class SetProteinCommandHandler @Inject constructor(
    private val configuration: ReadableConfiguration,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleSetProteinCommand(setProteinCommand: SetProteinCommand) {
        val eggIdShell = setProteinCommand.argument
        val slot = setProteinCommand.slot
        if (!passwordService.eggExists(eggIdShell, CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            CommandExecutionTracker.markFailure()
            finish(setProteinCommand)
            return
        }
        if (passwordService.proteinExists(eggIdShell, slot) && !commandConfirmed(setProteinCommand)) {
            abort(setProteinCommand)
            return
        }
        val typeInput = receiveTypeInput(setProteinCommand) ?: run {
            abort(setProteinCommand)
            return
        }
        val structureInput = try {
            structureInputReceived(secureInputDetermined())
        } catch (_: SecureInputUnavailableException) {
            abort(setProteinCommand)
            return
        }
        if (structureInput.isEmpty) {
            abort(setProteinCommand)
            return
        }
        if (passwordService.putProtein(
                eggIdShell = eggIdShell,
                slot = slot,
                typeShell = typeInput.shell,
                structureShell = structureInput.shell,
            ).failure
        ) {
            CommandExecutionTracker.markFailure()
        }
        finish(setProteinCommand)
    }

    private fun commandConfirmed(setProteinCommand: SetProteinCommand) = if (configuration.application.password.promptOnRemoval &&
        passwordService.eggExists(setProteinCommand.argument, PasswordService.EggNotExistsAction.DO_NOTHING)
    ) {
        val msg = "Existing Protein at Slot '${setProteinCommand.slot.index()}' of Egg '${setProteinCommand.argument.asString()}' " +
            "will be irrevocably overwritten.\nInput 'c' to confirm or anything else to abort.\nYour input: "
        userInterfaceAdapterPort.receiveConfirmation(Output.outputOf(Shell.shellOf(msg)))
    } else {
        true
    }

    private fun secureInputDetermined(): Boolean {
        val secureInput = configuration.domain.protein.secureProteinStructureInput
        if (configuration.domain.protein.promptForProteinStructureInputToggle) {
            val verb = if (secureInput) "Disable" else "Enable"
            if (userInterfaceAdapterPort.receiveYes(outputOf(shellOf("$verb secure input for next input? Y/n ")))) {
                return !secureInput
            }
        }
        return secureInput
    }

    private fun structureInputReceived(secureInput: Boolean) =
        with(outputOf(shellOf("Enter Protein Structure or just press enter to abort: "))) {
            when (secureInput) {
                true -> userInterfaceAdapterPort.receiveSecurely(this)
                false -> userInterfaceAdapterPort.receive(this)
            }
        }

    private fun receiveTypeInput(setProteinCommand: SetProteinCommand) = passwordService.viewProteinType(
        setProteinCommand.argument,
        setProteinCommand.slot,
    ).get().let { type ->
        val typeMsg = if (type.isEmpty) {
            "Enter Protein Type or just press enter to abort: "
        } else {
            "Enter new Protein Type to replace '${type.asString()}' or just press enter to keep it: "
        }
        userInterfaceAdapterPort.receive(outputOf(shellOf(typeMsg))).let { if (it.isEmpty) inputOf(type) else it }
    }.takeIf { it.isNotEmpty }

    private fun abort(setProteinCommand: SetProteinCommand) {
        CommandExecutionTracker.markAborted()
        userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
        finish(setProteinCommand)
    }

    private fun finish(setProteinCommand: SetProteinCommand) {
        setProteinCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
