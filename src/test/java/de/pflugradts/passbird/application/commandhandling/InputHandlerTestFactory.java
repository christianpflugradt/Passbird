package de.pflugradts.passbird.application.commandhandling;

import de.pflugradts.passbird.application.commandhandling.command.CommandFactory;
import de.pflugradts.passbird.application.commandhandling.command.namespace.NamespaceCommandFactory;
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler;
import de.pflugradts.passbird.application.failurehandling.FailureCollector;

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
