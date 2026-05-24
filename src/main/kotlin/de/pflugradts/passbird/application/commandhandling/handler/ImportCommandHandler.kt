package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.ImportCommand
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.exchange.ImportExportService
import de.pflugradts.passbird.application.exchange.ImportNestPreview
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.slotAt
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.HIGHLIGHT
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.PasswordService
import jakarta.inject.Inject

class ImportCommandHandler@Inject constructor(
    private val configuration: ReadableConfiguration,
    private val importExportService: ImportExportService,
    private val nestService: NestService,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleImportCommand(importCommand: ImportCommand) {
        when (if (importCommand.selective) selectiveCommandConfirmed() else commandConfirmed()) {
            ImportCommandConfirmation.CONFIRMED -> if (importCommand.selective) {
                val selectedNest = selectedNest ?: return
                importExportService.importEggs(selectedNest.slot, selectedNest.targetSlot)
            } else {
                importExportService.importEggs()
            }

            ImportCommandConfirmation.ABORTED ->
                userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))

            ImportCommandConfirmation.FAILED -> Unit
        }
        userInterfaceAdapterPort.sendLineBreak()
    }

    private var selectedNest: SelectedNest? = null

    private fun commandConfirmed(): ImportCommandConfirmation {
        selectedNest = null
        if (configuration.application.password.promptOnRemoval) {
            val importedEggIds = importExportService.peekImportEggIdShells()
            if (importedEggIds.failure) {
                return ImportCommandConfirmation.FAILED
            }
            val overlaps = importedEggIds.getOrNull()!!
                .map { (nestSlot, eggIdShell) -> eggIdShell.map { Triple(nestSlot, it, passwordService.eggExists(it, nestSlot)) } }
                .flatten()
                .filter { it.third }
                .map { Pair(it.first, it.second) }
            if (overlaps.isNotEmpty()) {
                return confirmImport(overlaps)
            }
        }
        return ImportCommandConfirmation.CONFIRMED
    }

    private fun selectiveCommandConfirmed(): ImportCommandConfirmation {
        selectedNest = null
        val importedNests = importExportService.peekImportNests()
        if (importedNests.failure) {
            return ImportCommandConfirmation.FAILED
        }
        val previews = importedNests.getOrNull()!!.takeIf(List<ImportNestPreview>::isNotEmpty)
            ?: return ImportCommandConfirmation.ABORTED
        userInterfaceAdapterPort.send(outputOf(shellOf("\nAvailable Nests in import file:\n"), HIGHLIGHT))
        userInterfaceAdapterPort.send(outputOf(shellOf(previews.joinToString("\n") { "\t${it.slot.index()}: ${it.nestId.asString()}" })))
        val sourceSlot = receiveSourceSlot(previews) ?: return ImportCommandConfirmation.ABORTED
        val targetSlot = receiveTargetSlot() ?: return ImportCommandConfirmation.ABORTED
        val preview = previews.first { it.slot == sourceSlot }
        if (targetSlot == DEFAULT && sourceSlot != DEFAULT) {
            return ImportCommandConfirmation.ABORTED
        }
        val targetNest = nestService.atNestSlot(targetSlot)
        if (targetNest.isPresent && targetNest.get().viewNestId() != preview.nestId) {
            return ImportCommandConfirmation.ABORTED
        }
        val overlaps = preview.eggIds
            .filter { eggId -> passwordService.eggExists(eggId, targetSlot) }
            .map { eggId -> Pair(targetSlot, eggId) }
        if (overlaps.isNotEmpty()) {
            val confirmation = confirmImport(overlaps)
            if (confirmation != ImportCommandConfirmation.CONFIRMED) {
                return confirmation
            }
        }
        selectedNest = SelectedNest(sourceSlot, targetSlot)
        return ImportCommandConfirmation.CONFIRMED
    }

    private fun receiveSourceSlot(previews: List<ImportNestPreview>) = receiveNestSlot(
        prompt = "\nSpecify a Nest Slot 0-9 to import or anything else to abort: ",
        availableSlots = previews.map { it.slot }.toSet(),
    )

    private fun receiveTargetSlot() = receiveNestSlot(
        prompt = "Specify a target Nest Slot 0-9 or anything else to abort: ",
        availableSlots = Slot.entries.toSet(),
    )

    private fun receiveNestSlot(prompt: String, availableSlots: Set<Slot>) = userInterfaceAdapterPort.receive(
        outputOf(shellOf(prompt)),
    ).shell.asString()
        .takeIf { input -> input.length == 1 && input[0].isDigit() }
        ?.let(::slotAt)
        ?.takeIf(availableSlots::contains)

    private fun confirmImport(overlaps: List<Pair<Slot, de.pflugradts.passbird.domain.model.shell.Shell>>) = if (
        userInterfaceAdapterPort.receiveConfirmation(
            outputOf(
                shellOf(
                    "By importing this file ${overlaps.size} existing Passwords " +
                        "will be irrevocably overwritten.\n" +
                        "The following Eggs will be affected: " +
                        "${overlaps.joinToString { "${it.second.asString()} (${it.first})" }}\n" +
                        "Input 'c' to confirm or anything else to abort.\nYour input: ",
                ),
            ),
        )
    ) {
        ImportCommandConfirmation.CONFIRMED
    } else {
        ImportCommandConfirmation.ABORTED
    }
}

private enum class ImportCommandConfirmation { CONFIRMED, ABORTED, FAILED }

private data class SelectedNest(val slot: Slot, val targetSlot: Slot)
