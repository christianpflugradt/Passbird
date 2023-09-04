package de.pflugradts.passbird.application.commandhandling.command;

import de.pflugradts.passbird.domain.model.transfer.Input;

public abstract class AbstractSingleCharInputCommand extends AbstractInputCommand {

    private static final int MAX_COMMAND_SIZE = 1;

    protected AbstractSingleCharInputCommand(final Input input) {
        super(input);
        if (input.getCommand().getSize() > MAX_COMMAND_SIZE) {
            throw new IllegalArgumentException("parameter for command '" + input.getCommand().getChar(0)
                + "' not supported: " + input.getCommand().slice(1).asString());
        }
    }

}
