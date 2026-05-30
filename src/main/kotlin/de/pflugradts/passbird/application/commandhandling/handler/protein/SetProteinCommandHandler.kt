package de.pflugradts.passbird.application.commandhandling.handler.protein
import de.pflugradts.passbird.application.SecureInputUnavailableException
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.SetProteinCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
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
class SetProteinCommandHandler constructor(
    private val configuration: ReadableConfiguration,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<SetProteinCommand>(SetProteinCommand::class.java) {
    override fun handleCommand(command: SetProteinCommand) {
        val eggIdShell = command.argument
        val slot = command.slot
        if (!passwordService.eggExists(eggIdShell, CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            commandExecutionTracker.markFailure()
            finish(command)
            return
        }
        if (passwordService.proteinExists(eggIdShell, slot) && !commandConfirmed(command)) {
            abort(command)
            return
        }
        val typeInput = receiveTypeInput(command) ?: run {
            abort(command)
            return
        }
        try {
            val structureInput = try {
                structureInputReceived(secureInputDetermined())
            } catch (_: SecureInputUnavailableException) {
                abort(command)
                return
            }
            try {
                if (structureInput.isEmpty) {
                    abort(command)
                    return
                }
                putProtein(eggIdShell, slot, typeInput, structureInput)
            } finally {
                structureInput.invalidate()
            }
        } finally {
            typeInput.invalidate()
        }
        finish(command)
    }
    private fun commandConfirmed(command: SetProteinCommand) = if (configuration.application.password.promptOnRemoval &&
        passwordService.eggExists(command.argument, PasswordService.EggNotExistsAction.DO_NOTHING)
    ) {
        val msg = "Existing Protein at Slot '${command.slot.index()}' of Egg '${command.argument.asString()}' " +
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
    private fun receiveTypeInput(command: SetProteinCommand): Input? {
        val currentType = passwordService.viewProteinType(
            command.argument,
            command.slot,
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
    private fun abort(command: SetProteinCommand) {
        commandExecutionTracker.markAborted()
        userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
        finish(command)
    }
    private fun finish(command: SetProteinCommand) {
        command.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
