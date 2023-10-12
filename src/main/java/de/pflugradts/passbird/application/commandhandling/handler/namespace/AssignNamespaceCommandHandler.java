package de.pflugradts.passbird.application.commandhandling.handler.namespace;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.commandhandling.command.namespace.AssignNamespaceCommand;
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.NamespaceService;
import de.pflugradts.passbird.domain.service.password.PasswordService;

import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.INVALID;
import static de.pflugradts.passbird.domain.service.password.PasswordService.EntryNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT;

public class AssignNamespaceCommandHandler implements CommandHandler, CanListAvailableNamespaces {

    @Inject
    private NamespaceService namespaceService;
    @Inject
    private PasswordService passwordService;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;

    @Subscribe
    private void handleAssignNamespaceCommand(final AssignNamespaceCommand assignNamespaceCommand) {
        if (passwordService.entryExists(assignNamespaceCommand.getArgument(), CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            userInterfaceAdapterPort.send(Output.Companion.outputOf(Bytes.bytesOf(String.format("Available namespaces: %n%s",
                getAvailableNamespaces(namespaceService, false)))));
            final var input = userInterfaceAdapterPort
                .receive(Output.Companion.outputOf(Bytes.bytesOf("Enter namespace you want to move password entry to: ")));
            final var namespace = input.parseNamespace();
            if (namespace == INVALID) {
                userInterfaceAdapterPort.send(Output.Companion.outputOf(Bytes.bytesOf(
                    "Invalid namespace - Operation aborted.")));
            } else if (namespace == namespaceService.getCurrentNamespace().getSlot()) {
                userInterfaceAdapterPort.send(Output.Companion.outputOf(Bytes.bytesOf(
                    "Password entry is already in the specified namespace - Operation aborted.")));
            } else if (namespaceService.atSlot(namespace).isEmpty()) {
                userInterfaceAdapterPort.send(Output.Companion.outputOf(Bytes.bytesOf(
                    "Specified namespace does not exist - Operation aborted.")));
            } else if (passwordService.entryExists(assignNamespaceCommand.getArgument(), namespace)) {
                userInterfaceAdapterPort.send(Output.Companion.outputOf(Bytes.bytesOf(
                    "Password entry with same alias already exists in target namespace - Operation aborted.")));
            } else {
                passwordService.movePasswordEntry(assignNamespaceCommand.getArgument(), namespace);
            }
            input.invalidate();
        }
        assignNamespaceCommand.invalidateInput();
        userInterfaceAdapterPort.sendLineBreak();
    }

}
