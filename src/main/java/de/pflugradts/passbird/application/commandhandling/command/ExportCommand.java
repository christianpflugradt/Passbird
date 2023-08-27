package de.pflugradts.passbird.application.commandhandling.command;

import de.pflugradts.passbird.domain.model.transfer.Input;

public class ExportCommand extends AbstractFilenameCommand {

    protected ExportCommand(final Input input) {
        super(input);
    }

}
