package de.pflugradts.pwman3.application.commandhandling.command;

import de.pflugradts.pwman3.domain.model.transfer.Input;

public class ViewCommand extends AbstractSingleCharInputCommand {

    protected ViewCommand(final Input input) {
        super(input);
    }

}
