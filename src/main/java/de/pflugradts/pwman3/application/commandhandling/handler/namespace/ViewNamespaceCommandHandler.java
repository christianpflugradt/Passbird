package de.pflugradts.pwman3.application.commandhandling.handler.namespace;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.commandhandling.command.namespace.ViewNamespaceCommand;
import de.pflugradts.pwman3.application.commandhandling.handler.CommandHandler;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import de.pflugradts.pwman3.domain.service.NamespaceService;

import java.util.Optional;
import java.util.stream.Collectors;

import static de.pflugradts.pwman3.domain.model.namespace.Namespace.DEFAULT;

public class ViewNamespaceCommandHandler implements CommandHandler {

    @Inject
    private NamespaceService namespaceService;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;

    @Subscribe
    private void handleViewNamespaceCommand(final ViewNamespaceCommand viewNamespaceCommand) {
        userInterfaceAdapterPort.send(Output.of(Bytes.of(String.format(
            "%nAttention: Namespaces are work in progress and of limited use until they're fully implemented%n"
                + "%nCurrent namespace: %s%n%n"
                + "Available namespaces: %s%n"
                + "Available namespace commands:%n"
                + "\tn (view) displays current namespace, available namespaces and namespace commands%n"
                + "\tn[1-9] (switch) switches to the namespace at the given slot (between 1 and 9 inclusively)%n"
                + "\tn[1-9][key] (assign) assigns the password for that key to the specified namespace%n"
                + "\tn+[1-9] (create) creates a new namespace for the specified slot%n"
                + "\tn-[1-9] (discard) discards the namespace for the specified slot",
            getCurrentNamespace(),
            getAvailableNamespaces()
        ))));
        userInterfaceAdapterPort.sendLineBreak();
    }

    private String getCurrentNamespace() {
        final var namespace = namespaceService.getCurrentNamespace();
        return (namespace == DEFAULT) ? "default" : namespace.getBytes().asString();
    }

    private String getAvailableNamespaces() {
        return namespaceService.all().anyMatch(Optional::isPresent)
            ? System.lineSeparator() + namespaceService.all()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(namespace -> "\t"
                    + namespace.getSlot().index()
                    + ": "
                    + namespace.getBytes().asString()
                    + System.lineSeparator())
                .collect(Collectors.joining())
            : "there aren't any namespaces, use the n+ command to create one" + System.lineSeparator();
    }

}
