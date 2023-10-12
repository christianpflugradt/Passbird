package de.pflugradts.passbird.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.commandhandling.command.RenameCommand;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.password.PasswordService;

import static de.pflugradts.passbird.domain.service.password.PasswordService.EntryNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT;

public class RenameCommandHandler implements CommandHandler {

    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Inject
    private PasswordService passwordService;

    @Subscribe
    private void handleRenameCommand(final RenameCommand renameCommand) {
        if (passwordService.entryExists(renameCommand.getArgument(), CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            final var secureInput = userInterfaceAdapterPort
                    .receive(Output.Companion.outputOf(Bytes.bytesOf("Enter new alias or nothing to abort: ")));
            if (secureInput.isEmpty()) {
                userInterfaceAdapterPort.send(Output.Companion.outputOf(Bytes.bytesOf("Empty input - Operation aborted.")));
            } else {
                passwordService.renamePasswordEntry(renameCommand.getArgument(), secureInput.getBytes());
            }
            secureInput.invalidate();
        }
        userInterfaceAdapterPort.sendLineBreak();
        renameCommand.invalidateInput();
    }

}
