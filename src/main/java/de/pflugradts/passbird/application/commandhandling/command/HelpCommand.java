package de.pflugradts.passbird.application.commandhandling.command;

import de.pflugradts.passbird.domain.model.transfer.Input;

public class HelpCommand extends AbstractSingleCharInputCommand {

    protected HelpCommand(final Input input) {
        super(input);
    }

}
