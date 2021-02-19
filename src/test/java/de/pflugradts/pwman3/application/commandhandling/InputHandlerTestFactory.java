package de.pflugradts.pwman3.application.commandhandling;

import de.pflugradts.pwman3.application.commandhandling.command.CommandFactory;
import de.pflugradts.pwman3.application.commandhandling.command.namespace.NamespaceCommandFactory;
import de.pflugradts.pwman3.application.commandhandling.handler.CommandHandler;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;

import java.util.Set;

class InputHandlerTestFactory {

    static InputHandler setupInputHandlerFor(final CommandBus commandBus) {
        return new InputHandler(
            commandBus,
            new CommandFactory(new NamespaceCommandFactory()),
            new FailureCollector());
    }

    static InputHandler setupInputHandlerFor(final CommandHandler commandHandler) {
        return new InputHandler(
            new CommandBus(null, Set.of(commandHandler)),
            new CommandFactory(new NamespaceCommandFactory()),
            new FailureCollector());
    }

}
