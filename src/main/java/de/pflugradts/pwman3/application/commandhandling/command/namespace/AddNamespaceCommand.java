package de.pflugradts.pwman3.application.commandhandling.command.namespace;

import de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot;

public class AddNamespaceCommand extends AbstractNamespaceSlotCommand {

    protected AddNamespaceCommand(final NamespaceSlot namespaceSlot) {
        super(namespaceSlot);
    }

}
