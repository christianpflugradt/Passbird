package de.pflugradts.pwman3.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.pwman3.application.ClipboardAdapterPort;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.commandhandling.command.GetCommand;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import de.pflugradts.pwman3.domain.service.password.PasswordService;

public class GetCommandHandler implements CommandHandler {

    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Inject
    private ClipboardAdapterPort clipboardAdapterPort;
    @Inject
    private PasswordService passwordService;

    @Subscribe
    private void handleGetCommand(final GetCommand getCommand) {
        passwordService.viewPassword(getCommand.getArgument()).ifPresent(
            passwordBytes -> clipboardAdapterPort.post(Output.of(passwordBytes))
        );
        userInterfaceAdapterPort.send(Output.of(Bytes.of("Password copied to clipboard.")));
        getCommand.invalidateInput();
        userInterfaceAdapterPort.sendLineBreak();
    }

}
