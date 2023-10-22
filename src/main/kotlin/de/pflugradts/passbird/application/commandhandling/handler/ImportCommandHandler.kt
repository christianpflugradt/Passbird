package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.collect.Streams
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.ImportCommand
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.exchange.ImportExportService
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.BytesComparator
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService
import java.util.stream.Collectors

class ImportCommandHandler@Inject constructor(
    @Inject private val configuration: ReadableConfiguration,
    @Inject private val importExportService: ImportExportService,
    @Inject private val passwordService: PasswordService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleImportCommand(importCommand: ImportCommand) {
        if (commandConfirmed(importCommand)) {
            importExportService.importPasswordEntries(importCommand.argument.asString())
        } else {
            userInterfaceAdapterPort.send(outputOf(bytesOf("Operation aborted.")))
        }
        importCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun commandConfirmed(importCommand: ImportCommand): Boolean {
        if (configuration.application.password.promptOnRemoval) {
            val duplicateDetector = HashSet<Bytes>()
            val overlaps = Streams.concat(
                importExportService.peekImportKeyBytes(importCommand.argument.asString()).distinct(),
                passwordService.findAllKeys().distinct(),
            )
                .filter { !duplicateDetector.add(it) }
                .sorted(BytesComparator())
                .map { it.asString() }
                .collect(Collectors.joining(", "))
            if (overlaps.isNotEmpty()) {
                return userInterfaceAdapterPort.receiveConfirmation(
                    outputOf(
                        bytesOf(
                            "By importing this file ${overlaps.chars().filter { it == ','.code }.count() + 1} existing Passwords" +
                                "will be irrevocably overwritten.\n" +
                                "The following Password Entries will be affected: ${overlaps}\n" +
                                "Input 'c' to confirm or anything else to abort.%nYour input: ",
                        ),
                    ),
                )
            }
        }
        return true
    }
}
