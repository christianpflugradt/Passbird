package de.pflugradts.passbird.application.commandhandling.command.namespace;

import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;

public class AddNamespaceCommand extends AbstractNamespaceSlotCommand {

    protected AddNamespaceCommand(final NamespaceSlot namespaceSlot) {
        super(namespaceSlot);
    }

}
