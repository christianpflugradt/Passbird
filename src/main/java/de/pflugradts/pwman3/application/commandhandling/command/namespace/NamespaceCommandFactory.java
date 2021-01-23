package de.pflugradts.pwman3.application.commandhandling.command.namespace;

import de.pflugradts.pwman3.application.commandhandling.command.Command;
import de.pflugradts.pwman3.application.commandhandling.command.NullCommand;
import de.pflugradts.pwman3.domain.model.transfer.Input;

public class NamespaceCommandFactory {

    public Command constructFromInput(final Input input) {
        final var command = input.getCommand();
        final var data = input.getData();
        if (command.size() == 1 && data.isEmpty()) {
            return new ViewNamespaceCommand();
        }
        return new NullCommand();
    }

}
