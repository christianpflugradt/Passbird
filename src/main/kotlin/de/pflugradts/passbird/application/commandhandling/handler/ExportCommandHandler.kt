package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.ExportCommand
import de.pflugradts.passbird.application.exchange.ImportExportService

class ExportCommandHandler @Inject constructor(
    @Inject private val importExportService: ImportExportService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleExportCommand(@Suppress("UNUSED_PARAMETER") exportCommand: ExportCommand) {
        importExportService.exportEggs()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
