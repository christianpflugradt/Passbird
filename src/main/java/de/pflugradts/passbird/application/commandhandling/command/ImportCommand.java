package de.pflugradts.passbird.application.commandhandling.command;

import de.pflugradts.passbird.domain.model.transfer.Input;

public class ImportCommand extends AbstractFilenameCommand {

    protected ImportCommand(final Input input) {
        super(input);
    }

}
