package de.pflugradts.pwman3.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.commandhandling.command.ViewCommand;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import de.pflugradts.pwman3.domain.service.PasswordService;

public class ViewCommandHandler implements CommandHandler {

    @Inject
    private PasswordService passwordService;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;

    @Subscribe
    private void handleViewCommand(final ViewCommand viewCommand) {
        passwordService.viewPassword(viewCommand.getArgument()).ifPresent(passwordBytes -> {
            userInterfaceAdapterPort.send(Output.of(passwordBytes));
        });
        viewCommand.invalidateInput();
        userInterfaceAdapterPort.sendLineBreak();
    }

}
