package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.ImportCommand
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.exchange.ImportExportService
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.NestService
import de.pflugradts.passbird.domain.service.password.PasswordService

class ImportCommandHandler@Inject constructor(
    @Inject private val configuration: ReadableConfiguration,
    @Inject private val importExportService: ImportExportService,
    @Inject private val nestService: NestService,
    @Inject private val passwordService: PasswordService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleImportCommand(importCommand: ImportCommand) {
        if (commandConfirmed(importCommand)) {
            importExportService.importEggs(importCommand.argument.asString())
        } else {
            userInterfaceAdapterPort.send(outputOf(bytesOf("Operation aborted.")))
        }
        importCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun commandConfirmed(importCommand: ImportCommand): Boolean {
        if (configuration.application.password.promptOnRemoval) {
            val overlaps = importExportService.peekImportEggIdBytes(importCommand.argument.asString())
                .map { (nestSlot, eggIdBytes) -> eggIdBytes.map { Triple(nestSlot, it, passwordService.eggExists(it, nestSlot)) } }
                .flatten()
                .filter { it.third }
                .map { Pair(it.first, it.second) }
            if (overlaps.isNotEmpty()) {
                return userInterfaceAdapterPort.receiveConfirmation(
                    outputOf(
                        bytesOf(
                            "By importing this file ${overlaps.size} existing Passwords" +
                                "will be irrevocably overwritten.\n" +
                                "The following Password Entries will be affected: " +
                                "${overlaps.joinToString { "${it.second.asString()} (${it.first})" }}\n" +
                                "Input 'c' to confirm or anything else to abort.%nYour input: ",
                        ),
                    ),
                )
            }
        }
        return true
    }
}
