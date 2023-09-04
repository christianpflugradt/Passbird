package de.pflugradts.passbird.application.commandhandling.handler.namespace;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.commandhandling.command.namespace.AddNamespaceCommand;
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler;
import de.pflugradts.passbird.application.failurehandling.FailureCollector;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.NamespaceService;

import static de.pflugradts.passbird.domain.model.namespace.Namespace.DEFAULT;

public class AddNamespaceCommandHandler implements CommandHandler {

    @Inject
    private NamespaceService namespaceService;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Inject
    private FailureCollector failureCollector;

    @Subscribe
    private void handleAddNamespaceCommand(final AddNamespaceCommand addNamespaceCommand) {
        if (namespaceService.atSlot(addNamespaceCommand.getSlot()).filter(DEFAULT::equals).isPresent()) {
            userInterfaceAdapterPort.send(Output.of(Bytes.bytesOf(
                "Default namespace cannot be replaced - Operation aborted.")));
            return;
        }
        final var prompt = namespaceService.atSlot(addNamespaceCommand.getSlot()).isPresent()
            ? String.format(
                "Enter new name for existing namespace '%s' or nothing to abort%nYour input: ",
                namespaceService.atSlot(addNamespaceCommand.getSlot()).get().getBytes().asString())
            : String.format("Enter name for namespace or nothing to abort%nYour input: ");
        final var input = userInterfaceAdapterPort
            .receive(Output.of(Bytes.bytesOf(prompt)));
        if (input.isEmpty()) {
            userInterfaceAdapterPort.send(Output.of(Bytes.bytesOf("Empty input - Operation aborted.")));
        } else {
            namespaceService.deploy(input.getBytes(), addNamespaceCommand.getSlot());
        }
        input.invalidate();
        userInterfaceAdapterPort.sendLineBreak();
    }

}
