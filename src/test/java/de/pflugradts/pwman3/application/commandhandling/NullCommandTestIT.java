package de.pflugradts.pwman3.application.commandhandling;

import de.pflugradts.pwman3.application.commandhandling.command.CommandFactory;
import de.pflugradts.pwman3.application.commandhandling.command.NullCommand;
import de.pflugradts.pwman3.application.commandhandling.command.namespace.NamespaceCommandFactory;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Input;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class NullCommandTestIT {

    @Spy
    private CommandBus commandBus = new CommandBus(null, Collections.emptySet());

    private InputHandler inputHandler;

    @BeforeEach
    private void setup() {
        inputHandler = new InputHandler(commandBus, new CommandFactory(new NamespaceCommandFactory()));
    }

    @Test
    void shouldHandleNullCommand() {
        // given
        final var input = Input.of(Bytes.of("?"));

        // when
        inputHandler.handleInput(input);

        // then
        then(commandBus).should().post(any(NullCommand.class));
    }

}
