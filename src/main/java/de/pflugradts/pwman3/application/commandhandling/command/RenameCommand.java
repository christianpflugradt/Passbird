package de.pflugradts.pwman3.application.commandhandling.command;

import de.pflugradts.pwman3.domain.model.transfer.Input;

public class RenameCommand extends AbstractSingleCharInputCommand {

    protected RenameCommand(final Input input) {
        super(input);
    }

}
