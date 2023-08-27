package de.pflugradts.passbird.application.commandhandling.command;

import de.pflugradts.passbird.domain.model.transfer.Input;

public class CustomSetCommand extends AbstractSingleCharInputCommand {

    protected CustomSetCommand(final Input input) {
        super(input);
    }

}
