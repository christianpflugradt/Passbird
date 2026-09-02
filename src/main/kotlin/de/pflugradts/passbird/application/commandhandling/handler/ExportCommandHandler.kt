package de.pflugradts.passbird.application.commandhandling.handler
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.capabilities.CanListAvailableNests
import de.pflugradts.passbird.application.commandhandling.command.ExportCommand
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.exchange.ImportExportService
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.slotAt
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.HIGHLIGHT
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.provider.PasswordProvider
import java.util.Arrays
class ExportCommandHandler constructor(
    private val canListAvailableNests: CanListAvailableNests,
    private val configuration: ReadableConfiguration,
    private val importExportService: ImportExportService,
    private val nestService: NestService,
    private val passwordProvider: PasswordProvider,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<ExportCommand>(ExportCommand::class.java) {
    override fun handleCommand(command: ExportCommand) {
        val exportPassword = when (val passwordPrompt = receivePassword()) {
            PasswordPromptResult.Aborted -> {
                sendAbortMessage()
                userInterfaceAdapterPort.sendLineBreak()
                return
            }

            PasswordPromptResult.Mismatched -> {
                userInterfaceAdapterPort.send(
                    outputOf(shellOf("Export passwords do not match. Operation aborted."), OPERATION_ABORTED),
                )
                userInterfaceAdapterPort.sendLineBreak()
                return
            }

            is PasswordPromptResult.Provided -> passwordPrompt.password
        }
        try {
            val exported = if (command.selective) {
                exportSelectedNests(exportPassword.value)
            } else {
                importExportService.exportEggs(exportPassword.value)
            }
            if (exported) {
                exportPassword.generated?.let { userInterfaceAdapterPort.send(outputOf(shellOf("Your export password: $it"))) }
            }
        } finally {
            Arrays.fill(exportPassword.value, '\u0000')
        }
        userInterfaceAdapterPort.sendLineBreak()
    }
    private fun exportSelectedNests(password: CharArray): Boolean {
        val availableNestSlots = availableNestSlots()
        userInterfaceAdapterPort.send(outputOf(shellOf("\nAvailable Nests:\n"), HIGHLIGHT))
        userInterfaceAdapterPort.send(outputOf(shellOf(canListAvailableNests.getAvailableNests(includeCurrent = true))))
        val selectionMode = receiveSelectionMode() ?: return abortExport()
        val selectedNestSlots = receiveSelectedNestSlots(availableNestSlots) ?: return abortExport()
        val exportedNestSlots = when (selectionMode) {
            ExportSelectionMode.SELECTED -> selectedNestSlots
            ExportSelectionMode.EXCEPT_SELECTED -> availableNestSlots - selectedNestSlots
        }
        if (exportedNestSlots.isEmpty()) {
            sendAbortMessage()
            return false
        }
        return importExportService.exportEggs(exportedNestSlots, password)
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
    private fun abortExport(): Boolean {
        sendAbortMessage()
        return false
    }
    private fun receivePassword(): PasswordPromptResult = when (
        userInterfaceAdapterPort.receive(
            outputOf(shellOf("Generate a random password to encrypt the export file? Y/n\nYour input: ")),
        ).shell.asString()
    ) {
        "", "y", "Y" -> passwordProvider.createNewPassword(configuration.parsePasswordRequirements()).let {
            PasswordPromptResult.Provided(ExportPassword(it.toChars(), it.asString()))
        }

        "n", "N" -> manualPassword()

        else -> PasswordPromptResult.Aborted
    }
    private fun manualPassword(): PasswordPromptResult {
        val password = userInterfaceAdapterPort.receiveSecurely(
            outputOf(shellOf("Input a password to encrypt the export file.\nYour input: ")),
        ).shell.toChars()
        val repeated = userInterfaceAdapterPort.receiveSecurely(
            outputOf(shellOf("Repeat the export password.\nYour input: ")),
        ).shell.toChars()
        val passwordsMatch = password.contentEquals(repeated)
        return try {
            when {
                password.isEmpty() && repeated.isEmpty() -> PasswordPromptResult.Aborted
                passwordsMatch -> PasswordPromptResult.Provided(ExportPassword(password))
                else -> PasswordPromptResult.Mismatched
            }
        } finally {
            Arrays.fill(repeated, '\u0000')
            if (!passwordsMatch) {
                Arrays.fill(password, '\u0000')
            }
        }
    }
}
private enum class ExportSelectionMode { SELECTED, EXCEPT_SELECTED }
private data class ExportPassword(val value: CharArray, val generated: String? = null)
private sealed interface PasswordPromptResult {
    data class Provided(val password: ExportPassword) : PasswordPromptResult
    data object Aborted : PasswordPromptResult
    data object Mismatched : PasswordPromptResult
}
private fun Shell.toChars() = CharArray(size) { getChar(it) }
