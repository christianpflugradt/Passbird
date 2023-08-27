package de.pflugradts.passbird.application.commandhandling.command;

import de.pflugradts.passbird.domain.model.transfer.Input;

public class ViewCommand extends AbstractSingleCharInputCommand {

    protected ViewCommand(final Input input) {
        super(input);
    }

}
