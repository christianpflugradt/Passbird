package de.pflugradts.pwman3.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.boot.Bootable;
import de.pflugradts.pwman3.application.commandhandling.command.QuitCommand;
import de.pflugradts.pwman3.application.util.SystemOperation;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Output;

public class QuitCommandHandler implements CommandHandler {

    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Inject
    private Bootable bootable;

    @Subscribe
    private void handleQuitCommand(final QuitCommand quitCommand) {
        userInterfaceAdapterPort.send(Output.of(Bytes.of("goodbye")));
        bootable.terminate(new SystemOperation());
    }

}
