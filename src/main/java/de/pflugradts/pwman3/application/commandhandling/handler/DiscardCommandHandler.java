package de.pflugradts.pwman3.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.commandhandling.command.DiscardCommand;
import de.pflugradts.pwman3.application.configuration.ReadableConfiguration;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import de.pflugradts.pwman3.domain.service.password.PasswordService;

public class DiscardCommandHandler implements CommandHandler {

    @Inject
    private FailureCollector failureCollector;
    @Inject
    private ReadableConfiguration configuration;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Inject
    private PasswordService passwordService;

    @Subscribe
    private void handleDiscardCommand(final DiscardCommand discardCommand) {
        if (commandConfirmed()) {
            passwordService.discardPasswordEntry(discardCommand.getArgument())
                    .onFailure(throwable -> failureCollector
                            .collectPasswordEntryFailure(discardCommand.getArgument(), throwable));
        } else {
            userInterfaceAdapterPort.send(Output.of(Bytes.of("Operation aborted.")));
        }
        discardCommand.invalidateInput();
        userInterfaceAdapterPort.sendLineBreak();
    }

    private boolean commandConfirmed() {
        if (configuration.getApplication().getPassword().isPromptOnRemoval()) {
            return userInterfaceAdapterPort
                    .receiveConfirmation(Output.of(Bytes.of(
                            "Discarding a Password Entry is an irrevocable action.\n"
                                    + "Input 'c' to confirm or anything else to abort.\nYour input: ")));
        }
        return true;
    }

}
