package de.pflugradts.passbird.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.commandhandling.command.CustomSetCommand;
import de.pflugradts.passbird.application.configuration.ReadableConfiguration;
import de.pflugradts.passbird.application.failurehandling.FailureCollector;
import de.pflugradts.passbird.domain.model.password.InvalidKeyException;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.password.PasswordService;

import static de.pflugradts.passbird.domain.service.password.PasswordService.EntryNotExistsAction.DO_NOTHING;

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
            try {
                passwordService.challengeAlias(customSetCommand.getArgument());
                final var secureInput = userInterfaceAdapterPort
                    .receiveSecurely(Output.Companion.outputOf(Bytes.bytesOf("Enter custom password: ")));
                if (secureInput.isEmpty()) {
                    userInterfaceAdapterPort.send(Output.Companion.outputOf(Bytes.bytesOf("Empty input - Operation aborted.")));
                } else {
                    passwordService.putPasswordEntry(customSetCommand.getArgument(), secureInput.getBytes());
                }
                secureInput.invalidate();
            } catch (InvalidKeyException ex) {
                userInterfaceAdapterPort.send(Output.Companion.outputOf(Bytes.bytesOf("Password alias cannot contain digits or special characters. Please choose a different alias.")));
            }
        } else {
            userInterfaceAdapterPort.send(Output.Companion.outputOf(Bytes.bytesOf("Operation aborted.")));
        }
        customSetCommand.invalidateInput();
        userInterfaceAdapterPort.sendLineBreak();
    }

    private boolean commandConfirmed(final CustomSetCommand customSetCommand) {
        if (configuration.getApplication().getPassword().isPromptOnRemoval()
                && passwordService.entryExists(customSetCommand.getArgument(), DO_NOTHING)) {
            return userInterfaceAdapterPort
                    .receiveConfirmation(Output.Companion.outputOf(Bytes.bytesOf(String.format(
                            "Existing Password Entry '%s' will be irrevocably overwritten.%n"
                                    + "Input 'c' to confirm or anything else to abort.%nYour input: ",
                            customSetCommand.getArgument().asString()))));
        }
        return true;
    }

}
