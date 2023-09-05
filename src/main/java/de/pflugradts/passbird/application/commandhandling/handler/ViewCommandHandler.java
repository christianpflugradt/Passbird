package de.pflugradts.passbird.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.commandhandling.command.ViewCommand;
import de.pflugradts.passbird.application.failurehandling.FailureCollector;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.password.PasswordService;

public class ViewCommandHandler implements CommandHandler {

    @Inject
    private FailureCollector failureCollector;
    @Inject
    private PasswordService passwordService;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;

    @Subscribe
    private void handleViewCommand(final ViewCommand viewCommand) {
        passwordService.viewPassword(viewCommand.getArgument()).ifPresent(result -> result
                .onFailure(throwable -> failureCollector
                        .collectPasswordEntryFailure(viewCommand.getArgument(), throwable))
                .onSuccess(passwordBytes -> userInterfaceAdapterPort.send(Output.Companion.outputOf(passwordBytes))));
        viewCommand.invalidateInput();
        userInterfaceAdapterPort.sendLineBreak();
    }

}
