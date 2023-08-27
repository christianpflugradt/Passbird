package de.pflugradts.passbird.application.commandhandling.command;

import de.pflugradts.passbird.domain.model.transfer.Input;

public class DiscardCommand extends AbstractSingleCharInputCommand {

    protected DiscardCommand(final Input input) {
        super(input);
    }

}
