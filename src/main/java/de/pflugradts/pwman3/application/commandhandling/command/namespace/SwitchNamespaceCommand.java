package de.pflugradts.pwman3.application.commandhandling.command.namespace;

import de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot;

public class SwitchNamespaceCommand extends AbstractNamespaceSlotCommand {

    protected SwitchNamespaceCommand(final NamespaceSlot namespaceSlot) {
        super(namespaceSlot);
    }

}
