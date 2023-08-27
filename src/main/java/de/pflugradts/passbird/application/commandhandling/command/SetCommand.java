package de.pflugradts.passbird.application.commandhandling.command;

import de.pflugradts.passbird.domain.model.transfer.Input;

public class SetCommand extends AbstractSingleCharInputCommand {

    protected SetCommand(final Input input) {
        super(input);
    }

}
