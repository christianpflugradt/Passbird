package de.pflugradts.pwman3.application.commandhandling;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.pflugradts.pwman3.application.commandhandling.command.CommandFactory;
import de.pflugradts.pwman3.application.commandhandling.command.CommandType;
import de.pflugradts.pwman3.domain.model.transfer.Input;
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
        commandBus.post(commandFactory.construct(CommandType.fromChar(input.getCommandChar()), input));
    }

}
