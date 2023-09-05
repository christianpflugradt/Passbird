package de.pflugradts.passbird.application.commandhandling.handler;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.boot.Bootable;
import de.pflugradts.passbird.application.commandhandling.command.QuitCommand;
import de.pflugradts.passbird.application.util.SystemOperation;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Output;

public class QuitCommandHandler implements CommandHandler {

    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Inject
    private Bootable bootable;

    @Subscribe
    private void handleQuitCommand(final QuitCommand quitCommand) {
        userInterfaceAdapterPort.send(Output.Companion.outputOf(Bytes.bytesOf("goodbye")));
        bootable.terminate(new SystemOperation());
    }

}
