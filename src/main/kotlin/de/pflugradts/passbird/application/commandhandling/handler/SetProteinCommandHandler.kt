package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.SetProteinCommand
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT

class SetProteinCommandHandler @Inject constructor(
    @Inject private val configuration: ReadableConfiguration,
    @Inject private val passwordService: PasswordService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleSetProteinCommand(setProteinCommand: SetProteinCommand) {
        val eggIdShell = setProteinCommand.argument
        val slot = setProteinCommand.slot
        if (passwordService.eggExists(eggIdShell, CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            if (!passwordService.proteinExists(eggIdShell, slot) || commandConfirmed(setProteinCommand)) {
                val type = passwordService.viewProteinType(setProteinCommand.argument, setProteinCommand.slot).get()
                val typeMsg = if (type.isEmpty) {
                    "Enter Protein Type: "
                } else {
                    "Enter new Protein Type to replace '${type.asString()}' or just press enter to keep it: "
                }
                val typeInput = userInterfaceAdapterPort.receive(outputOf(shellOf(typeMsg))).let { if (it.isEmpty) inputOf(type) else it }
                val structureMsg = "Enter Protein Structure or just press enter to abort: "
                val structureInput = userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf(structureMsg)))
                if (structureInput.isNotEmpty) {
                    passwordService.putProtein(
                        eggIdShell = eggIdShell,
                        slot = slot,
                        typeShell = typeInput.shell,
                        structureShell = structureInput.shell,
                    )
                } else {
                    userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
                }
            }
        }
        setProteinCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
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
}
