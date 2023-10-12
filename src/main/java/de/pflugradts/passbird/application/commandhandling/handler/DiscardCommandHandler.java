package de.pflugradts.passbird.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.commandhandling.command.DiscardCommand;
import de.pflugradts.passbird.application.configuration.ReadableConfiguration;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.password.PasswordService;

public class DiscardCommandHandler implements CommandHandler {

    @Inject
    private ReadableConfiguration configuration;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Inject
    private PasswordService passwordService;

    @Subscribe
    private void handleDiscardCommand(final DiscardCommand discardCommand) {
        if (commandConfirmed()) {
            passwordService.discardPasswordEntry(discardCommand.getArgument());
        } else {
            userInterfaceAdapterPort.send(Output.Companion.outputOf(Bytes.bytesOf("Operation aborted.")));
        }
        discardCommand.invalidateInput();
        userInterfaceAdapterPort.sendLineBreak();
    }

    private boolean commandConfirmed() {
        if (configuration.getApplication().getPassword().getPromptOnRemoval()) {
            return userInterfaceAdapterPort
                    .receiveConfirmation(Output.Companion.outputOf(Bytes.bytesOf(
                            "Discarding a Password Entry is an irrevocable action.\n"
                                    + "Input 'c' to confirm or anything else to abort.\nYour input: ")));
        }
        return true;
    }

}
