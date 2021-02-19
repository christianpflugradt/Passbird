package de.pflugradts.pwman3.application.commandhandling.command;

import de.pflugradts.pwman3.domain.model.transfer.Input;

public class HelpCommand extends AbstractSingleCharInputCommand {

    protected HelpCommand(final Input input) {
        super(input);
    }

}
