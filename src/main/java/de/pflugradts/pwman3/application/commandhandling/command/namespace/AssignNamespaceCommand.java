package de.pflugradts.pwman3.application.commandhandling.command.namespace;

import de.pflugradts.pwman3.application.commandhandling.command.AbstractSingleCharInputCommand;
import de.pflugradts.pwman3.domain.model.transfer.Input;

public class AssignNamespaceCommand extends AbstractSingleCharInputCommand {

    protected AssignNamespaceCommand(final Input input) {
        super(input);
    }

}
