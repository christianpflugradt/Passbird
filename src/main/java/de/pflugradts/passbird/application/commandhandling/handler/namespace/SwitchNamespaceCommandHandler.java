package de.pflugradts.passbird.application.commandhandling.handler.namespace;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.commandhandling.command.namespace.SwitchNamespaceCommand;
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.NamespaceService;

public class SwitchNamespaceCommandHandler implements CommandHandler {

    @Inject
    private NamespaceService namespaceService;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;

    @Subscribe
    private void handleSwitchNamespaceCommand(final SwitchNamespaceCommand switchNamespaceCommand) {
        if (namespaceService.getCurrentNamespace().getSlot().equals(switchNamespaceCommand.getSlot())) {
            userInterfaceAdapterPort.send(Output.of(Bytes.bytesOf("'"
                + namespaceService.getCurrentNamespace().getBytes().asString()
                + "' is already the current namespace.")));
        } else if (namespaceService.atSlot(switchNamespaceCommand.getSlot()).isPresent()) {
            namespaceService.updateCurrentNamespace(switchNamespaceCommand.getSlot());
        } else {
            userInterfaceAdapterPort.send(Output.of(Bytes.bytesOf(
                "Specified namespace does not exist - Operation aborted.")));
        }
        userInterfaceAdapterPort.sendLineBreak();
    }

}
