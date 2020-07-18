package de.pflugradts.pwman3.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.commandhandling.command.SetCommand;
import de.pflugradts.pwman3.application.configuration.ReadableConfiguration;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import de.pflugradts.pwman3.domain.service.password.provider.PasswordProvider;
import de.pflugradts.pwman3.domain.service.password.PasswordService;

public class SetCommandHandler implements CommandHandler {

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
            passwordService.putPasswordEntry(
                    setCommand.getArgument(),
                    passwordProvider.createNewPassword(configuration.parsePasswordRequirements()));
        } else {
            userInterfaceAdapterPort.send(Output.of(Bytes.of("Operation aborted.")));
        }
        setCommand.invalidateInput();
        userInterfaceAdapterPort.sendLineBreak();
    }

    private boolean commandConfirmed(final SetCommand setCommand) {
        if (configuration.getApplication().getPassword().isPromptOnRemoval()
                && passwordService.entryExists(setCommand.getArgument())) {
            return userInterfaceAdapterPort
                    .receiveConfirmation(Output.of(Bytes.of(String.format(
                            "Existing Password Entry '%s' will be irrevocably overwritten.%n"
                                    + "Input 'c' to confirm or anything else to abort.%nYour input: ",
                            setCommand.getArgument().asString()))));
        }
        return true;
    }

}

