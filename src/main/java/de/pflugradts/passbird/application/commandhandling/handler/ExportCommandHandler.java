package de.pflugradts.passbird.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.commandhandling.command.ExportCommand;
import de.pflugradts.passbird.application.exchange.ImportExportService;

public class ExportCommandHandler implements CommandHandler {

    @Inject
    private ImportExportService importExportService;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;

    @Subscribe
    private void handleExportCommand(final ExportCommand exportCommand) {
        importExportService.exportPasswordEntries(exportCommand.getArgument().asString());
        exportCommand.invalidateInput();
        userInterfaceAdapterPort.sendLineBreak();
    }

}
