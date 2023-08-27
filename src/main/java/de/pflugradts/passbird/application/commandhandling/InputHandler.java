package de.pflugradts.passbird.application.commandhandling;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.pflugradts.passbird.application.commandhandling.command.CommandFactory;
import de.pflugradts.passbird.application.commandhandling.command.CommandType;
import de.pflugradts.passbird.application.commandhandling.command.NullCommand;
import de.pflugradts.passbird.application.failurehandling.FailureCollector;
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
    @Inject
    private FailureCollector failureCollector;

    public void handleInput(final Input input) {
        commandBus.post(
            commandFactory.construct(CommandType.fromCommandBytes(input.getCommand()), input)
                .onFailure(failureCollector::collectCommandFailure)
                .getOrElse(NullCommand::new)
        );
    }

}
