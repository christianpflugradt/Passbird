package de.pflugradts.passbird.application.commandhandling.command;

import de.pflugradts.passbird.domain.model.transfer.Input;

public class GetCommand extends AbstractSingleCharInputCommand {

    protected GetCommand(final Input input) {
        super(input);
    }

}
