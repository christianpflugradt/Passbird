package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.ExportCommand
import de.pflugradts.passbird.application.exchange.ImportExportService
import jakarta.inject.Inject

class ExportCommandHandler @Inject constructor(
    private val importExportService: ImportExportService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleExportCommand(@Suppress("UNUSED_PARAMETER") exportCommand: ExportCommand) {
        importExportService.exportEggs()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
