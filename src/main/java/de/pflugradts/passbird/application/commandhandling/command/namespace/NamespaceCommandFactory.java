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
        if (command.getSize() > MAX_COMMAND_SIZE) {
            throw new IllegalArgumentException("namespace command parameter not supported: "
                + input.getCommand().slice(2).asString());
        } else if (command.getSize() == 1 && data.isEmpty()) {
            return new ViewNamespaceCommand();
        } else if (command.getSize() == 1 && !data.isEmpty()) {
            return new AssignNamespaceCommand(input);
        } else if (command.getSize() == 2 && CharValue.Companion.charValueOf(command.getChar(1)).isDigit()) {
            return new SwitchNamespaceCommand(NamespaceSlot.Companion.at(command.getChar(1)));
        } else if (command.getSize() > 2
                && command.getChar(1) == ADD.getValue()) {
            return new AddNamespaceCommand(NamespaceSlot.Companion.at(command.getChar(2)));
        }
        return new NullCommand();
    }

}
