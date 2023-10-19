package de.pflugradts.passbird.application.commandhandling.handler.namespace;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.commandhandling.command.ViewNamespaceCommand;
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.FixedNamespaceService;

public class ViewNamespaceCommandHandler implements CommandHandler, CanListAvailableNamespaces {

    @Inject
    private FixedNamespaceService namespaceService;
    @Inject
    private UserInterfaceAdapterPort userInterfaceAdapterPort;

    @Subscribe
    private void handleViewNamespaceCommand(final ViewNamespaceCommand viewNamespaceCommand) {
        userInterfaceAdapterPort.send(Output.Companion.outputOf(Bytes.bytesOf(String.format(
                "%nCurrent namespace: %s%n%n"
                + "Available namespaces: %n%s%n"
                + "Available namespace commands:%n"
                + "\tn (view) displays current namespace, available namespaces and namespace commands%n"
                + "\tn0 (switch to default) switches to the default namespace%n"
                + "\tn[1-9] (switch) switches to the namespace at the given slot (between 1 and 9 inclusively)%n"
                + "\tn[1-9][key] (assign) assigns the password for that key to the specified namespace%n"
                + "\tn+[1-9] (create) creates a new namespace at the specified slot%n"
                + "\t[NOT YET IMPLEMENTED] n-[1-9] (discard) discards the namespace at the specified slot",
            getCurrentNamespace(),
            getAvailableNamespaces()
        ))));
        userInterfaceAdapterPort.sendLineBreak();
    }

    private String getCurrentNamespace() {
        return namespaceService.getCurrentNamespace().getBytes().asString();
    }

    private String getAvailableNamespaces() {
        final var namespaceList = getAvailableNamespaces(namespaceService, true);
        return hasCustomNamespaces(namespaceService)
            ? namespaceList
            : namespaceList + "\t(use the n+ command to create custom namespaces)" + System.lineSeparator();
    }

}
