package de.pflugradts.pwman3.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.pwman3.application.commandhandling.command.DiscardCommand;
import de.pflugradts.pwman3.domain.service.PasswordService;

public class DiscardCommandHandler implements CommandHandler {

    @Inject
    private PasswordService passwordService;

    @Subscribe
    private void handleDiscardCommand(final DiscardCommand discardCommand) {
        passwordService.discardPasswordEntry(discardCommand.getArgument());
        discardCommand.invalidateInput();
    }

}
