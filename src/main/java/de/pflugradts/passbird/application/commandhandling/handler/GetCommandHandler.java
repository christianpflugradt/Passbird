package de.pflugradts.passbird.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.passbird.application.ClipboardAdapterPort;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.commandhandling.command.GetCommand;
import de.pflugradts.passbird.application.failurehandling.FailureCollector;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.password.PasswordService;

public class GetCommandHandler implements CommandHandler {

    @Inject
    private FailureCollector failureCollector;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Inject
    private ClipboardAdapterPort clipboardAdapterPort;
    @Inject
    private PasswordService passwordService;

    @Subscribe
    private void handleGetCommand(final GetCommand getCommand) {
        passwordService.viewPassword(getCommand.getArgument()).ifPresent(result -> result
                .onFailure(throwable -> failureCollector
                        .collectPasswordEntryFailure(getCommand.getArgument(), throwable))
                .onSuccess(passwordBytes -> {
                    clipboardAdapterPort.post(Output.of(passwordBytes));
                    userInterfaceAdapterPort.send(Output.of(Bytes.of("Password copied to clipboard.")));
                }));
        getCommand.invalidateInput();
        userInterfaceAdapterPort.sendLineBreak();
    }

}