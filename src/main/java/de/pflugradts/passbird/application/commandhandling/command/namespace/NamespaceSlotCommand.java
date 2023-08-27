package de.pflugradts.passbird.application.commandhandling.command.namespace;

import de.pflugradts.passbird.application.commandhandling.command.Command;
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;

public interface NamespaceSlotCommand extends Command {

    NamespaceSlot getSlot();

}
