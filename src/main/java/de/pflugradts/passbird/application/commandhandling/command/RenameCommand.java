package de.pflugradts.passbird.application.commandhandling.command;

import de.pflugradts.passbird.domain.model.transfer.Input;

public class RenameCommand extends AbstractSingleCharInputCommand {

    protected RenameCommand(final Input input) {
        super(input);
    }

}
