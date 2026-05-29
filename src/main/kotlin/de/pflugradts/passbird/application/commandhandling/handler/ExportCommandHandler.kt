package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.capabilities.CanListAvailableNests
import de.pflugradts.passbird.application.commandhandling.command.ExportCommand
import de.pflugradts.passbird.application.exchange.ImportExportService
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.slotAt
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.HIGHLIGHT
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.nest.NestService
import jakarta.inject.Inject

class ExportCommandHandler @Inject constructor(
    private val canListAvailableNests: CanListAvailableNests,
    private val importExportService: ImportExportService,
    private val nestService: NestService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : CommandHandler {
    @Subscribe
    private fun handleExportCommand(exportCommand: ExportCommand) {
        if (exportCommand.selective) {
            exportSelectedNests()
        } else {
            importExportService.exportEggs()
        }
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun exportSelectedNests() {
        val availableNestSlots = availableNestSlots()
        userInterfaceAdapterPort.send(outputOf(shellOf("\nAvailable Nests:\n"), HIGHLIGHT))
        userInterfaceAdapterPort.send(outputOf(shellOf(canListAvailableNests.getAvailableNests(includeCurrent = true))))
        val selectionMode = receiveSelectionMode() ?: return sendAbortMessage()
        val selectedNestSlots = receiveSelectedNestSlots(availableNestSlots) ?: return sendAbortMessage()
        val exportedNestSlots = when (selectionMode) {
            ExportSelectionMode.SELECTED -> selectedNestSlots
            ExportSelectionMode.EXCEPT_SELECTED -> availableNestSlots - selectedNestSlots
        }
        if (exportedNestSlots.isEmpty()) {
            sendAbortMessage()
            return
        }
        importExportService.exportEggs(exportedNestSlots)
    }

    private fun availableNestSlots() = nestService.all(includeDefault = true)
        .filter { it.isPresent }
        .map { it.get().slot }
        .toList()
        .toSet()

    private fun receiveSelectionMode() = userInterfaceAdapterPort.receive(
        outputOf(shellOf("\nInput 1 to export only selected Nests or 2 to export all Nests except selected Nests.\nYour input: ")),
    ).shell.asString().let {
        when (it) {
            "1" -> ExportSelectionMode.SELECTED
            "2" -> ExportSelectionMode.EXCEPT_SELECTED
            else -> null
        }
    }

    private fun receiveSelectedNestSlots(availableNestSlots: Set<Slot>) = userInterfaceAdapterPort.receive(
        outputOf(shellOf("Specify Nest Slots separated by ','.\nYour input: ")),
    ).shell.asString().split(',')
        .map { it.trim() }
        .takeIf { parts -> parts.isNotEmpty() && parts.all { part -> part.length == 1 && part[0].isDigit() } }
        ?.map { part -> slotAt(part) }
        ?.toSet()
        ?.takeIf { selectedSlots -> selectedSlots.all(availableNestSlots::contains) }

    private fun sendAbortMessage() {
        commandExecutionTracker.markAborted()
        userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
    }
}

private enum class ExportSelectionMode { SELECTED, EXCEPT_SELECTED }
