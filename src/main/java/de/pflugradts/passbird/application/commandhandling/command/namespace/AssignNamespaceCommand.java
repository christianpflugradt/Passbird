package de.pflugradts.passbird.application.commandhandling.command.namespace;

import de.pflugradts.passbird.application.commandhandling.command.base.AbstractSingleCharInputCommand;
import de.pflugradts.passbird.domain.model.transfer.Input;

public class AssignNamespaceCommand extends AbstractSingleCharInputCommand {

    protected AssignNamespaceCommand(final Input input) {
        super(input);
    }

}
