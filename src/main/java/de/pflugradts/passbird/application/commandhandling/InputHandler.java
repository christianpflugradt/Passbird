package de.pflugradts.passbird.application.commandhandling;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.pflugradts.passbird.application.commandhandling.command.CommandFactory;
import de.pflugradts.passbird.application.commandhandling.command.CommandType;
import de.pflugradts.passbird.domain.model.transfer.Input;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Singleton
public class InputHandler {

    @Inject
    private CommandBus commandBus;
    @Inject
    private CommandFactory commandFactory;

    public void handleInput(final Input input) {
        commandBus.post(
            commandFactory.construct(CommandType.fromCommandBytes(input.getCommand()), input)
        );
    }

}
