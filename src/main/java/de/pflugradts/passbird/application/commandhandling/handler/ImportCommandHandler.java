package de.pflugradts.passbird.application.commandhandling.handler;

import com.google.common.collect.Streams;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.commandhandling.command.ImportCommand;
import de.pflugradts.passbird.application.configuration.ReadableConfiguration;
import de.pflugradts.passbird.application.exchange.ImportExportService;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.BytesComparator;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.password.PasswordService;

import java.util.HashSet;
import java.util.stream.Collectors;

public class ImportCommandHandler implements CommandHandler {

    @Inject
    private ReadableConfiguration configuration;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Inject
    private PasswordService passwordService;
    @Inject
    private ImportExportService importExportService;

    @Subscribe
    private void handleImportCommand(final ImportCommand importCommand) {
        if (commandConfirmed(importCommand)) {
            importExportService.importPasswordEntries(importCommand.getArgument().asString());
        } else {
            userInterfaceAdapterPort.send(Output.Companion.outputOf(Bytes.bytesOf("Operation aborted.")));
        }
        importCommand.invalidateInput();
        userInterfaceAdapterPort.sendLineBreak();
    }

    private boolean commandConfirmed(final ImportCommand importCommand) {
        if (configuration.getApplication().getPassword().getPromptOnRemoval()) {
            final var duplicateDetector = new HashSet<Bytes>();
            final var overlaps =
                    Streams.concat(
                            importExportService.peekImportKeyBytes(importCommand.getArgument().asString()).distinct(),
                            passwordService.findAllKeys().distinct())
                    .filter(bytes -> !duplicateDetector.add(bytes))
                    .sorted(new BytesComparator())
                    .map(Bytes::asString)
                    .collect(Collectors.joining(", "));
            if (!overlaps.isEmpty()) {
                return userInterfaceAdapterPort
                        .receiveConfirmation(Output.Companion.outputOf(Bytes.bytesOf(String
                        .format(
                                "By importing this file %d existing Passwords will be irrevocably overwritten.%n"
                                        + "The following Password Entries will be affected: %s%n"
                                        + "Input 'c' to confirm or anything else to abort.%nYour input: ",
                                overlaps.chars().filter(chr -> chr == ',').count() + 1,
                                overlaps))));
            }
        }
        return true;
    }

}
