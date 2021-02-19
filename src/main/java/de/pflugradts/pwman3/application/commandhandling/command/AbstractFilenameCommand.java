package de.pflugradts.pwman3.application.commandhandling.command;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Input;

public abstract class AbstractFilenameCommand extends AbstractInputCommand {

    protected AbstractFilenameCommand(final Input input) {
        super(input);
    }

    @Override
    public Bytes getArgument() {
        return getInput().getBytes().slice(1);
    }

}
