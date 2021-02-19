package de.pflugradts.pwman3.application.commandhandling.command;

import de.pflugradts.pwman3.domain.model.transfer.Input;

public abstract class AbstractSingleCharInputCommand extends AbstractInputCommand {

    private static final int MAX_COMMAND_SIZE = 1;

    protected AbstractSingleCharInputCommand(final Input input) {
        super(input);
        if (input.getCommand().size() > MAX_COMMAND_SIZE) {
            throw new IllegalArgumentException("parameter for command '" + input.getCommand().getChar(0)
                + "' not supported: " + input.getCommand().slice(1).asString());
        }
    }

}
