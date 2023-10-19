package de.pflugradts.passbird.application.commandhandling.handler.namespace;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.commandhandling.command.SwitchNamespaceCommand;
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.FixedNamespaceService;

public class SwitchNamespaceCommandHandler implements CommandHandler {

    @Inject
    private FixedNamespaceService namespaceService;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;

    @Subscribe
    private void handleSwitchNamespaceCommand(final SwitchNamespaceCommand switchNamespaceCommand) {
        if (namespaceService.getCurrentNamespace().getSlot().equals(switchNamespaceCommand.getSlot())) {
            userInterfaceAdapterPort.send(Output.Companion.outputOf(Bytes.bytesOf("'"
                + namespaceService.getCurrentNamespace().getBytes().asString()
                + "' is already the current namespace.")));
        } else if (namespaceService.atSlot(switchNamespaceCommand.getSlot()).isPresent()) {
            namespaceService.updateCurrentNamespace(switchNamespaceCommand.getSlot());
        } else {
            userInterfaceAdapterPort.send(Output.Companion.outputOf(Bytes.bytesOf(
                "Specified namespace does not exist - Operation aborted.")));
        }
        userInterfaceAdapterPort.sendLineBreak();
    }

}
