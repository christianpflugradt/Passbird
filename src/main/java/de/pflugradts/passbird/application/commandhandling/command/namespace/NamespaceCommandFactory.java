package de.pflugradts.passbird.application.commandhandling.command.namespace;

import de.pflugradts.passbird.application.commandhandling.command.Command;
import de.pflugradts.passbird.application.commandhandling.command.NullCommand;
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;
import de.pflugradts.passbird.domain.model.transfer.CharValue;
import de.pflugradts.passbird.domain.model.transfer.Input;

import static de.pflugradts.passbird.application.commandhandling.command.CommandVariant.ADD;

public class NamespaceCommandFactory {

    private static final int MAX_COMMAND_SIZE = 3;

    @SuppressWarnings("PMD.CyclomaticComplexity")
    public Command constructFromInput(final Input input) {
        final var command = input.getCommand();
        final var data = input.getData();
        if (command.size() > MAX_COMMAND_SIZE) {
            throw new IllegalArgumentException("namespace command parameter not supported: "
                + input.getCommand().slice(2).asString());
        } else if (command.size() == 1 && data.isEmpty()) {
            return new ViewNamespaceCommand();
        } else if (command.size() == 1 && !data.isEmpty()) {
            return new AssignNamespaceCommand(input);
        } else if (command.size() == 2 && CharValue.of(command.getChar(1)).isDigit()) {
            return new SwitchNamespaceCommand(NamespaceSlot.at(command.getChar(1)));
        } else if (command.size() > 2
                && command.getChar(1) == ADD.getValue()) {
            return new AddNamespaceCommand(NamespaceSlot.at(command.getChar(2)));
        }
        return new NullCommand();
    }

}
