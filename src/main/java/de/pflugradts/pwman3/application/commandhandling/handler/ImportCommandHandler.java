package de.pflugradts.pwman3.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.pwman3.application.commandhandling.command.ImportCommand;
import de.pflugradts.pwman3.application.exchange.ImportExportService;

public class ImportCommandHandler implements CommandHandler {

    @Inject
    private ImportExportService importExportService;

    @Subscribe
    private void handleImportCommand(final ImportCommand importCommand) {
        importExportService.imp(importCommand.getArgument().asString());
        importCommand.invalidateInput();
    }

}
