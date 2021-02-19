package de.pflugradts.pwman3.application.commandhandling.command;

import de.pflugradts.pwman3.domain.model.transfer.Input;

public class DiscardCommand extends AbstractSingleCharInputCommand {

    protected DiscardCommand(final Input input) {
        super(input);
    }

}
