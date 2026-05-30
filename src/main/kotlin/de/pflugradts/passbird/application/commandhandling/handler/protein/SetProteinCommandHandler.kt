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
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.transfer.Input
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
    private val commandExecutionTracker: CommandExecutionTracker,
) : CommandHandler {
    @Subscribe
    private fun handleSetProteinCommand(setProteinCommand: SetProteinCommand) {
        val eggIdShell = setProteinCommand.argument
        val slot = setProteinCommand.slot
        if (!passwordService.eggExists(eggIdShell, CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            commandExecutionTracker.markFailure()
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
        try {
            val structureInput = try {
                structureInputReceived(secureInputDetermined())
            } catch (_: SecureInputUnavailableException) {
                abort(setProteinCommand)
                return
            }
            try {
                if (structureInput.isEmpty) {
                    abort(setProteinCommand)
                    return
                }
                putProtein(eggIdShell, slot, typeInput, structureInput)
            } finally {
                structureInput.invalidate()
            }
        } finally {
            typeInput.invalidate()
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

    private fun receiveTypeInput(setProteinCommand: SetProteinCommand): Input? {
        val currentType = passwordService.viewProteinType(
            setProteinCommand.argument,
            setProteinCommand.slot,
        ).get()
        var selectedInput: Input? = null
        return try {
            val typeMsg = if (currentType.isEmpty) {
                "Enter Protein Type or just press enter to abort: "
            } else {
                "Enter new Protein Type to replace '${currentType.asString()}' or just press enter to keep it: "
            }
            selectedInput =
                userInterfaceAdapterPort.receive(outputOf(shellOf(typeMsg))).let { if (it.isEmpty) inputOf(currentType) else it }
            selectedInput.takeIf { it.isNotEmpty }
        } finally {
            if (selectedInput?.shell !== currentType) {
                currentType.scramble()
            }
        }
    }

    private fun putProtein(eggIdShell: Shell, slot: Slot, typeInput: Input, structureInput: Input) {
        val typeShell = typeInput.shell.copy()
        val structureShell = structureInput.shell.copy()
        try {
            if (passwordService.putProtein(
                    eggIdShell = eggIdShell,
                    slot = slot,
                    typeShell = typeShell,
                    structureShell = structureShell,
                ).failure
            ) {
                commandExecutionTracker.markFailure()
            }
        } finally {
            typeShell.scramble()
            structureShell.scramble()
        }
    }

    private fun abort(setProteinCommand: SetProteinCommand) {
        commandExecutionTracker.markAborted()
        userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
        finish(setProteinCommand)
    }

    private fun finish(setProteinCommand: SetProteinCommand) {
        setProteinCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
