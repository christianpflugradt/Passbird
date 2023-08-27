package de.pflugradts.passbird.application.commandhandling.command;

import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Input;

public abstract class AbstractFilenameCommand extends AbstractInputCommand {

    protected AbstractFilenameCommand(final Input input) {
        super(input);
    }

    @Override
    public Bytes getArgument() {
        return getInput().getBytes().slice(1);
    }

}
