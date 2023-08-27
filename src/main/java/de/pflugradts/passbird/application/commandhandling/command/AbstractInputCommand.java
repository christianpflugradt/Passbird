package de.pflugradts.passbird.application.commandhandling.command;

import de.pflugradts.passbird.domain.model.transfer.Input;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import lombok.Getter;

public abstract class AbstractInputCommand implements Command {

    @Getter
    private final Input input;

    protected AbstractInputCommand(final Input input) {
        this.input = input;
    }

    public Bytes getArgument() {
        return getInput().getData();
    }

    public void invalidateInput() {
        getInput().invalidate();
    }

}
