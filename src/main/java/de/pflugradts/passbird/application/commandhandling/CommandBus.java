package de.pflugradts.passbird.application.commandhandling;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.pflugradts.passbird.application.commandhandling.command.Command;
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler;

import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Singleton
public class CommandBus {

    private EventBus eventBus;

    @Inject
    private Set<CommandHandler> commandHandlers;

    public void post(final Command command) {
        getEventBus().post(command);
    }

    private void initializeEventBus() {
        eventBus = new EventBus();
        commandHandlers.forEach(commandHandler -> eventBus.register(commandHandler));
    }

    private EventBus getEventBus() {
        if (Objects.isNull(eventBus)) {
            initializeEventBus();
        }
        return eventBus;
    }

}
