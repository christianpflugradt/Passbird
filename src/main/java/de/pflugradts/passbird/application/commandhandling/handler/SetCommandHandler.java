package de.pflugradts.passbird.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.commandhandling.command.SetCommand;
import de.pflugradts.passbird.application.configuration.ReadableConfiguration;
import de.pflugradts.passbird.application.failurehandling.FailureCollector;
import de.pflugradts.passbird.domain.model.password.InvalidKeyException;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.password.provider.PasswordProvider;
import de.pflugradts.passbird.domain.service.password.PasswordService;

import static de.pflugradts.passbird.domain.service.password.PasswordService.EntryNotExistsAction.DO_NOTHING;

public class SetCommandHandler implements CommandHandler {

    @Inject
    private FailureCollector failureCollector;
    @Inject
    private ReadableConfiguration configuration;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Inject
    private PasswordService passwordService;
    @Inject
    private PasswordProvider passwordProvider;

    @Subscribe
    private void handleSetCommand(final SetCommand setCommand) {
        if (commandConfirmed(setCommand)) {
            try {
                passwordService.challengeAlias(setCommand.getArgument());
                passwordService.putPasswordEntry(setCommand.getArgument(),
                        passwordProvider.createNewPassword(configuration.parsePasswordRequirements())
                );
            } catch (InvalidKeyException ex) {
                userInterfaceAdapterPort.send(Output.Companion.outputOf(Bytes.bytesOf("Password alias cannot contain digits or special characters. Please choose a different alias.")));
            }
        } else {
            userInterfaceAdapterPort.send(Output.Companion.outputOf(Bytes.bytesOf("Operation aborted.")));
        }
        setCommand.invalidateInput();
        userInterfaceAdapterPort.sendLineBreak();
    }

    private boolean commandConfirmed(final SetCommand setCommand) {
        if (configuration.getApplication().getPassword().getPromptOnRemoval()
                && passwordService.entryExists(setCommand.getArgument(), DO_NOTHING)) {
            return userInterfaceAdapterPort
                    .receiveConfirmation(Output.Companion.outputOf(Bytes.bytesOf(String.format(
                            "Existing Password Entry '%s' will be irrevocably overwritten.%n"
                                    + "Input 'c' to confirm or anything else to abort.%nYour input: ",
                            setCommand.getArgument().asString()))));
        } else {
            return true;
        }
    }

}

