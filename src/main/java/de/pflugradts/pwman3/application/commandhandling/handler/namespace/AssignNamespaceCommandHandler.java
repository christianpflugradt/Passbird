package de.pflugradts.pwman3.application.commandhandling.handler.namespace;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.commandhandling.command.namespace.AssignNamespaceCommand;
import de.pflugradts.pwman3.application.commandhandling.handler.CommandHandler;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Input;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import de.pflugradts.pwman3.domain.service.NamespaceService;
import de.pflugradts.pwman3.domain.service.password.PasswordService;

import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot.INVALID;
import static de.pflugradts.pwman3.domain.service.password.PasswordService.EntryNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT;

public class AssignNamespaceCommandHandler implements CommandHandler, CanListAvailableNamespaces {

    @Inject
    private NamespaceService namespaceService;
    @Inject
    private PasswordService passwordService;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Inject
    private FailureCollector failureCollector;

    @Subscribe
    private void handleAssignNamespaceCommand(final AssignNamespaceCommand assignNamespaceCommand) {
        if (passwordService.entryExists(assignNamespaceCommand.getArgument(), CREATE_ENTRY_NOT_EXISTS_EVENT)
                .onFailure(throwable ->
                    failureCollector.collectPasswordEntryFailure(assignNamespaceCommand.getArgument(), throwable))
                .getOrElse(false)) {
            userInterfaceAdapterPort.send(Output.of(Bytes.of(String.format("Available namespaces: %n%s",
                getAvailableNamespaces(namespaceService, false)))));
            final var input = userInterfaceAdapterPort
                .receive(Output.of(Bytes.of("Enter namespace you want to move password entry to: ")))
                .onFailure(failureCollector::collectInputFailure)
                .getOrElse(Input.empty());
            final var namespace = input.parseNamespace();
            if (namespace == INVALID) {
                userInterfaceAdapterPort.send(Output.of(Bytes.of(
                    "Invalid namespace - Operation aborted.")));
            } else if (namespace == namespaceService.getCurrentNamespace().getSlot()) {
                userInterfaceAdapterPort.send(Output.of(Bytes.of(
                    "Password entry is already in the specified namespace - Operation aborted.")));
            } else if (namespaceService.atSlot(namespace).isEmpty()) {
                userInterfaceAdapterPort.send(Output.of(Bytes.of(
                    "Specified namespace does not exist - Operation aborted.")));
            } else if (passwordService.entryExists(
                assignNamespaceCommand.getArgument(), namespace).getOrElse(false)) {
                userInterfaceAdapterPort.send(Output.of(Bytes.of(
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
