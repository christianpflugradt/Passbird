package de.pflugradts.pwman3.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.pwman3.application.commandhandling.command.ExportCommand;
import de.pflugradts.pwman3.application.exchange.ImportExportService;

public class ExportCommandHandler implements CommandHandler {

    @Inject
    private ImportExportService importExportService;

    @Subscribe
    private void handleExportCommand(final ExportCommand exportCommand) {
        importExportService.exp(exportCommand.getArgument().asString());
        exportCommand.invalidateInput();
    }

}
