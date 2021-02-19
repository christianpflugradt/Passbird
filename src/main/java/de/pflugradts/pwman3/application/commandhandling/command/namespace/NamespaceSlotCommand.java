package de.pflugradts.pwman3.application.commandhandling.command.namespace;

import de.pflugradts.pwman3.application.commandhandling.command.Command;
import de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot;

public interface NamespaceSlotCommand extends Command {

    NamespaceSlot getSlot();

}
