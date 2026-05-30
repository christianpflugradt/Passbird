package de.pflugradts.passbird.application.commandhandling.handler
import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.ImportCommand
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.exchange.ImportExportService
import de.pflugradts.passbird.application.exchange.ImportNestPreview
import de.pflugradts.passbird.application.exchange.ShellMap
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.slotAt
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.HIGHLIGHT
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.PasswordService
class ImportCommandHandler constructor(
    private val configuration: ReadableConfiguration,
    private val importExportService: ImportExportService,
    private val nestService: NestService,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
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

            ImportCommandConfirmation.ABORTED -> {
                commandExecutionTracker.markAborted()
                userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
            }

            ImportCommandConfirmation.FAILED -> commandExecutionTracker.markFailure()
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
            return importedEggIds.getOrNull()!!.useScrambled {
                val overlaps = it
                    .map { (nestSlot, eggIdShell) -> eggIdShell.map { Triple(nestSlot, it, passwordService.eggExists(it, nestSlot)) } }
                    .flatten()
                    .filter { it.third }
                    .map { Pair(it.first, it.second) }
                if (overlaps.isNotEmpty()) {
                    confirmImport(overlaps)
                } else {
                    ImportCommandConfirmation.CONFIRMED
                }
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
        return previews.useScrambled {
            userInterfaceAdapterPort.send(outputOf(shellOf("\nAvailable Nests in import file:\n"), HIGHLIGHT))
            userInterfaceAdapterPort.send(outputOf(shellOf(it.joinToString("\n") { "\t${it.slot.index()}: ${it.nestId.asString()}" })))
            val sourceSlot = receiveSourceSlot(it) ?: return@useScrambled ImportCommandConfirmation.ABORTED
            val targetSlot = receiveTargetSlot() ?: return@useScrambled ImportCommandConfirmation.ABORTED
            val preview = it.first { it.slot == sourceSlot }
            if (targetSlot == DEFAULT && sourceSlot != DEFAULT) {
                return@useScrambled ImportCommandConfirmation.ABORTED
            }
            val targetNest = nestService.atNestSlot(targetSlot)
            if (targetNest.isPresent && targetNest.get().viewNestId() != preview.nestId) {
                return@useScrambled ImportCommandConfirmation.ABORTED
            }
            val overlaps = preview.eggIds
                .filter { eggId -> passwordService.eggExists(eggId, targetSlot) }
                .map { eggId -> Pair(targetSlot, eggId) }
            if (overlaps.isNotEmpty()) {
                val confirmation = confirmImport(overlaps)
                if (confirmation != ImportCommandConfirmation.CONFIRMED) {
                    return@useScrambled confirmation
                }
            }
            selectedNest = SelectedNest(sourceSlot, targetSlot)
            ImportCommandConfirmation.CONFIRMED
        }
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
    private fun confirmImport(overlaps: List<Pair<Slot, Shell>>) = if (
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
private inline fun <T> ShellMap.useScrambled(block: (ShellMap) -> T): T = try {
    block(this)
} finally {
    values.flatten().scrambleShells()
}
private inline fun <T> List<ImportNestPreview>.useScrambled(block: (List<ImportNestPreview>) -> T): T = try {
    block(this)
} finally {
    forEach {
        it.nestId.scramble()
        it.eggIds.scrambleShells()
    }
}
private fun Iterable<Shell>.scrambleShells() = forEach(Shell::scramble)
