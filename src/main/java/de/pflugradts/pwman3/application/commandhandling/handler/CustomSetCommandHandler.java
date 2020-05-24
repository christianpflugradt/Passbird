package de.pflugradts.pwman3.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.commandhandling.command.CustomSetCommand;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Input;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import de.pflugradts.pwman3.domain.service.PasswordService;

public class CustomSetCommandHandler implements CommandHandler {

    @Inject
    private FailureCollector failureCollector;
    @Inject
    private PasswordService passwordService;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;

    @Subscribe
    private void handleCustomSetCommand(final CustomSetCommand customSetCommand) {
        final var secureInput = userInterfaceAdapterPort
                .receiveSecurely(Output.of(Bytes.of("Enter custom password: ")))
                .onFailure(failureCollector::acceptInputFailure)
                .getOrElse(Input.empty());
        passwordService.putPasswordEntry(customSetCommand.getArgument(), secureInput.getBytes());
        customSetCommand.invalidateInput();
        secureInput.invalidate();
    }

}
