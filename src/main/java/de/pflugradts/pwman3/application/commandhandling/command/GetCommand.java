package de.pflugradts.pwman3.application.commandhandling.command;

import de.pflugradts.pwman3.domain.model.transfer.Input;

public class GetCommand extends AbstractSingleCharInputCommand {

    protected GetCommand(final Input input) {
        super(input);
    }

}
