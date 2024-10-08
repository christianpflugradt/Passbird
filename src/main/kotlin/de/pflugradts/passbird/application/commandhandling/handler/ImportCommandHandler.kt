package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.ImportCommand
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.exchange.ImportExportService
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.password.PasswordService

class ImportCommandHandler@Inject constructor(
    private val configuration: ReadableConfiguration,
    private val importExportService: ImportExportService,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleImportCommand(@Suppress("UNUSED_PARAMETER") importCommand: ImportCommand) {
        if (commandConfirmed()) {
            importExportService.importEggs()
        } else {
            userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
        }
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun commandConfirmed(): Boolean {
        if (configuration.application.password.promptOnRemoval) {
            val overlaps = importExportService.peekImportEggIdShells()
                .map { (nestSlot, eggIdShell) -> eggIdShell.map { Triple(nestSlot, it, passwordService.eggExists(it, nestSlot)) } }
                .flatten()
                .filter { it.third }
                .map { Pair(it.first, it.second) }
            if (overlaps.isNotEmpty()) {
                return userInterfaceAdapterPort.receiveConfirmation(
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
            }
        }
        return true
    }
}
