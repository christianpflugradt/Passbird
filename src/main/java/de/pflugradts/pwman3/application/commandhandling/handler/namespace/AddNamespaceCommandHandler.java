package de.pflugradts.pwman3.application.commandhandling.handler.namespace;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.commandhandling.command.namespace.AddNamespaceCommand;
import de.pflugradts.pwman3.application.commandhandling.handler.CommandHandler;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Input;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import de.pflugradts.pwman3.domain.service.NamespaceService;

import static de.pflugradts.pwman3.domain.model.namespace.Namespace.DEFAULT;

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
            userInterfaceAdapterPort.send(Output.of(Bytes.of(
                "Default namespace cannot be replaced - Operation aborted.")));
            return;
        }
        final var prompt = namespaceService.atSlot(addNamespaceCommand.getSlot()).isPresent()
            ? String.format(
                "Enter new name for existing namespace '%s' or nothing to abort%nYour input: ",
                namespaceService.atSlot(addNamespaceCommand.getSlot()).get().getBytes().asString())
            : String.format("Enter name for namespace or nothing to abort%nYour input: ");
        final var input = userInterfaceAdapterPort
            .receive(Output.of(Bytes.of(prompt)))
            .onFailure(failureCollector::collectInputFailure)
            .getOrElse(Input.empty());
        if (input.isEmpty()) {
            userInterfaceAdapterPort.send(Output.of(Bytes.of("Empty input - Operation aborted.")));
        } else {
            namespaceService.deploy(input.getBytes(), addNamespaceCommand.getSlot());
        }
        input.invalidate();
        userInterfaceAdapterPort.sendLineBreak();
    }

}
