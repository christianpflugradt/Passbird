package de.pflugradts.pwman3.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.commandhandling.command.RenameCommand;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Input;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import de.pflugradts.pwman3.domain.service.password.PasswordService;

import static de.pflugradts.pwman3.domain.service.password.PasswordService.EntryNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT;

public class RenameCommandHandler implements CommandHandler {

    @Inject
    private FailureCollector failureCollector;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Inject
    private PasswordService passwordService;

    @Subscribe
    private void handleRenameCommand(final RenameCommand renameCommand) {
        if (passwordService.entryExists(renameCommand.getArgument(), CREATE_ENTRY_NOT_EXISTS_EVENT)
                .onFailure(throwable ->
                        failureCollector.collectPasswordEntryFailure(renameCommand.getArgument(), throwable))
                .getOrElse(false)) {
            final var secureInput = userInterfaceAdapterPort
                    .receiveSecurely(Output.of(Bytes.of("Enter new alias or nothing to abort: ")))
                    .onFailure(failureCollector::collectInputFailure)
                    .getOrElse(Input.empty());
            if (secureInput.isEmpty()) {
                userInterfaceAdapterPort.send(Output.of(Bytes.of("Empty input - Operation aborted.")));
            } else {
                passwordService.renamePasswordEntry(renameCommand.getArgument(), secureInput.getBytes())
                    .onFailure(failureCollector::collectRenamePasswordEntryFailure);
            }
            secureInput.invalidate();
        }
        userInterfaceAdapterPort.sendLineBreak();
        renameCommand.invalidateInput();
    }

}
