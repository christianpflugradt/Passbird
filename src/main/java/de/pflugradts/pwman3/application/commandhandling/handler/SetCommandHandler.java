package de.pflugradts.pwman3.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.pwman3.domain.service.PasswordProvider;
import de.pflugradts.pwman3.application.commandhandling.command.SetCommand;
import de.pflugradts.pwman3.domain.service.PasswordService;

public class SetCommandHandler implements CommandHandler {

    @Inject
    private PasswordService passwordService;
    @Inject
    private PasswordProvider passwordProvider;

    @Subscribe
    private void handleSetCommand(final SetCommand setCommand) {
        passwordService.putPasswordEntry(
                setCommand.getArgument(),
                passwordProvider.createNewPassword());
        setCommand.invalidateInput();
    }

}

