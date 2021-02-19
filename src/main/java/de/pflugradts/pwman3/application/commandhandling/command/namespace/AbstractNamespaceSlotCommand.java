package de.pflugradts.pwman3.application.commandhandling.command.namespace;

import de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractNamespaceSlotCommand implements NamespaceSlotCommand {

    private final NamespaceSlot namespaceSlot;

    @Override
    public NamespaceSlot getSlot() {
        return namespaceSlot;
    }

}
