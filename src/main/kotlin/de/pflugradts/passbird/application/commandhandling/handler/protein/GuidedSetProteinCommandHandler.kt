package de.pflugradts.passbird.application.commandhandling.handler.protein

import de.pflugradts.passbird.application.SecureInputUnavailableException
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.GuidedSetProteinCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.eventhandling.ProteinEventOutputControl
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.EVENT_HANDLED
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT
import de.pflugradts.passbird.domain.service.password.ProteinEntry

class GuidedSetProteinCommandHandler constructor(
    private val configuration: ReadableConfiguration,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val proteinEventOutputControl: ProteinEventOutputControl,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<GuidedSetProteinCommand>(GuidedSetProteinCommand::class.java) {
    override fun handleCommand(command: GuidedSetProteinCommand) {
        val eggIdShell = command.argument
        if (!passwordService.eggExists(eggIdShell, CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            commandExecutionTracker.markFailure()
            finish(command)
            return
        }
        val selection = receiveTemplateSelection() ?: run {
            abort(command, "Operation aborted.")
            return
        }
        val template = if (selection == 0) {
            null
        } else {
            configuration.domain.protein.templates.getOrNull(selection - 1)
        }
        if (selection != 0 && template == null) {
            abort(command, "Specified template does not exist - Operation aborted.")
            return
        }
        if (template != null && template.slots.isEmpty()) {
            abort(command, "Specified template is empty - Operation aborted.")
            return
        }
        val proteinEntries = try {
            if (template == null) {
                collectUntemplatedProteinEntries(command)
            } else {
                collectTemplatedProteinEntries(command, template)
            }
        } catch (_: SecureInputUnavailableException) {
            abort(command, "Operation aborted.")
            return
        }
        try {
            if (proteinEntries.isEmpty()) {
                userInterfaceAdapterPort.send(
                    outputOf(shellOf("No Proteins were updated for egg '${eggIdShell.asString()}'."), EVENT_HANDLED),
                )
                finish(command)
                return
            }
            proteinEventOutputControl.suppress {
                if (passwordService.putProteins(eggIdShell, proteinEntries).failure) {
                    commandExecutionTracker.markFailure()
                    return@suppress
                }
                userInterfaceAdapterPort.send(
                    outputOf(shellOf("Proteins for egg '${eggIdShell.asString()}' successfully updated."), EVENT_HANDLED),
                )
            }
            finish(command)
        } finally {
            proteinEntries.forEach {
                it.typeShell.scramble()
                it.structureShell.scramble()
            }
        }
    }

    private fun receiveTemplateSelection(): Int? {
        userInterfaceAdapterPort.send(outputOf(shellOf("Available Protein templates:")))
        userInterfaceAdapterPort.send(outputOf(shellOf("\t0 none")))
        configuration.domain.protein.templates.forEachIndexed { index, template ->
            userInterfaceAdapterPort.send(outputOf(shellOf("\t${index + 1} ${template.name}")))
        }
        val selectionInput = userInterfaceAdapterPort.receive(outputOf(shellOf("Enter template index or just press enter to abort: ")))
        return try {
            selectionInput.takeUnless { it.isEmpty }?.shell?.asString()?.takeIf(::isExactIndex)?.toIntOrNull()
        } finally {
            selectionInput.invalidate()
        }
    }

    private fun collectUntemplatedProteinEntries(command: GuidedSetProteinCommand): List<ProteinEntry> = Slot.entries.mapNotNull { slot ->
        userInterfaceAdapterPort.send(outputOf(shellOf("Slot ${slot.index()}")))
        collectUntemplatedProteinEntry(command, slot)
    }

    private fun collectUntemplatedProteinEntry(command: GuidedSetProteinCommand, slot: Slot): ProteinEntry? {
        val currentType = currentType(command, slot)
        val currentStructure = currentStructure(command, slot)
        return try {
            val typeInput = receiveUntemplatedTypeInput(currentType)
            val nextType = when {
                currentType.isEmpty && typeInput.isEmpty -> return null
                typeInput.isEmpty -> currentType.copy()
                else -> typeInput.shell.copy()
            }
            try {
                val structureInput = receiveUntemplatedStructureInput(currentType, currentStructure, typeInput)
                val nextStructure = when {
                    currentType.isEmpty && structureInput.isEmpty -> return null
                    structureInput.isEmpty -> currentStructure.copy()
                    else -> structureInput.shell.copy()
                }
                ProteinEntry(slot, nextType, nextStructure)
            } finally {
                typeInput.invalidate()
            }
        } finally {
            currentType.scramble()
            currentStructure.scramble()
        }
    }

    private fun receiveUntemplatedTypeInput(currentType: Shell): Input {
        val prompt = if (currentType.isEmpty) {
            "Enter Protein Type or just press enter to continue: "
        } else {
            "Enter new Protein Type to replace '${currentType.asString()}' or just press enter to keep it: "
        }
        return userInterfaceAdapterPort.receive(outputOf(shellOf(prompt)))
    }

    private fun receiveUntemplatedStructureInput(currentType: Shell, currentStructure: Shell, typeInput: Input): Input {
        val prompt = when {
            currentType.isEmpty -> "Enter Protein Structure or just press enter to continue: "
            typeInput.isEmpty -> "Enter new Protein Structure to replace existing value or just press enter to keep it: "
            else -> "Enter new Protein Structure or just press enter to keep the existing value: "
        }
        return receiveStructureInput(prompt)
    }

    private fun collectTemplatedProteinEntries(
        command: GuidedSetProteinCommand,
        template: ReadableConfiguration.ProteinTemplate,
    ): List<ProteinEntry> = template.slots.toSortedMap().mapNotNull { (index, configuredType) ->
        val slot = Slot.slotAt(index)
        userInterfaceAdapterPort.send(outputOf(shellOf("Slot ${slot.index()} Type '$configuredType'")))
        collectTemplatedProteinEntry(command, slot, configuredType)
    }

    private fun collectTemplatedProteinEntry(command: GuidedSetProteinCommand, slot: Slot, configuredType: String): ProteinEntry? {
        val currentType = currentType(command, slot)
        val currentStructure = currentStructure(command, slot)
        return try {
            val slotOccupied = currentType.isNotEmpty
            val typeMatches = slotOccupied && currentType.asString() == configuredType
            if (slotOccupied) {
                val notice = if (typeMatches) {
                    "Existing Protein Structure at Slot '${slot.index()}' of Egg '${command.argument.asString()}' will be overwritten if you enter a new value."
                } else {
                    "Existing Protein Type and Structure at Slot '${slot.index()}' of Egg '${command.argument.asString()}' will be overwritten if you enter a new value."
                }
                userInterfaceAdapterPort.send(outputOf(shellOf(notice)))
            }
            val prompt = when {
                !slotOccupied -> "Enter Protein Structure for Type '$configuredType': "

                typeMatches ->
                    "Enter new Protein Structure for Type '$configuredType' or just press enter to keep the existing value: "

                else ->
                    "Enter new Protein Structure for Type '$configuredType' or just press enter to keep the existing Type and Structure: "
            }
            val structureInput = receiveStructureInput(prompt)
            try {
                when {
                    !slotOccupied && structureInput.isEmpty -> null
                    slotOccupied && structureInput.isEmpty -> null
                    else -> ProteinEntry(slot, shellOf(configuredType), structureInput.shell.copy())
                }
            } finally {
                structureInput.invalidate()
            }
        } finally {
            currentType.scramble()
            currentStructure.scramble()
        }
    }

    private fun receiveStructureInput(prompt: String): Input {
        val secureInput = secureInputDetermined()
        val output = outputOf(shellOf(prompt))
        return when (secureInput) {
            true -> userInterfaceAdapterPort.receiveSecurely(output)
            false -> userInterfaceAdapterPort.receive(output)
        }
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

    private fun currentType(command: GuidedSetProteinCommand, slot: Slot) =
        passwordService.viewProteinType(command.argument, slot).get().copy()

    private fun currentStructure(command: GuidedSetProteinCommand, slot: Slot) =
        passwordService.viewProteinStructure(command.argument, slot).get().copy()

    private fun abort(command: GuidedSetProteinCommand, message: String) {
        commandExecutionTracker.markAborted()
        userInterfaceAdapterPort.send(outputOf(shellOf(message), OPERATION_ABORTED))
        finish(command)
    }

    private fun finish(command: GuidedSetProteinCommand) {
        command.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun isExactIndex(input: String) = input.isNotEmpty() && input.all(Char::isDigit) && (input == "0" || !input.startsWith('0'))
}
