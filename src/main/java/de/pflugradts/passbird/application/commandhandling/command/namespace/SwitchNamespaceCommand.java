package de.pflugradts.passbird.application.commandhandling.command.namespace;

import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;

public class SwitchNamespaceCommand extends AbstractNamespaceSlotCommand {

    protected SwitchNamespaceCommand(final NamespaceSlot namespaceSlot) {
        super(namespaceSlot);
    }

}
