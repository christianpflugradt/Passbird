package de.pflugradts.pwman3.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.commandhandling.command.CustomSetCommand;
import de.pflugradts.pwman3.application.configuration.ReadableConfiguration;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Input;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import de.pflugradts.pwman3.domain.service.password.PasswordService;

public class CustomSetCommandHandler implements CommandHandler {

    @Inject
    private ReadableConfiguration configuration;
    @Inject
    private FailureCollector failureCollector;
    @Inject
    private PasswordService passwordService;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;

    @Subscribe
    private void handleCustomSetCommand(final CustomSetCommand customSetCommand) {
        if (commandConfirmed(customSetCommand)) {
            final var secureInput = userInterfaceAdapterPort
                    .receiveSecurely(Output.of(Bytes.of("Enter custom password: ")))
                    .onFailure(failureCollector::collectInputFailure)
                    .getOrElse(Input.empty());
            if (secureInput.isEmpty()) {
                userInterfaceAdapterPort.send(Output.of(Bytes.of("Empty input - Operation aborted.")));
            } else {
                passwordService.putPasswordEntry(customSetCommand.getArgument(), secureInput.getBytes());
            }
            secureInput.invalidate();
        } else {
            userInterfaceAdapterPort.send(Output.of(Bytes.of("Operation aborted.")));
        }
        customSetCommand.invalidateInput();
        userInterfaceAdapterPort.sendLineBreak();
    }

    private boolean commandConfirmed(final CustomSetCommand customSetCommand) {
        if (configuration.getApplication().getPassword().isPromptOnRemoval()
                && passwordService.entryExists(customSetCommand.getArgument())
                .onFailure(throwable -> failureCollector
                        .collectPasswordEntryFailure(customSetCommand.getArgument(), throwable))
                .getOrElse(false)) {
            return userInterfaceAdapterPort
                    .receiveConfirmation(Output.of(Bytes.of(String.format(
                            "Existing Password Entry '%s' will be irrevocably overwritten.%n"
                                    + "Input 'c' to confirm or anything else to abort.%nYour input: ",
                            customSetCommand.getArgument().asString()))));
        }
        return true;
    }

}
