package de.pflugradts.passbird.application.commandhandling;

import de.pflugradts.passbird.application.commandhandling.command.NullCommand;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Input;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static de.pflugradts.passbird.application.commandhandling.InputHandlerTestFactory.setupInputHandlerFor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class NullCommandTestIT {

    @Spy
    private final CommandBus commandBus = new CommandBus(null, Collections.emptySet());

    private InputHandler inputHandler;

    @BeforeEach
    void setup() {
        inputHandler = setupInputHandlerFor(commandBus);
    }

    @Test
    void shouldHandleUnknownCommand() {
        // given
        final var input =  Input.Companion.inputOf(Bytes.bytesOf("?"));

        // when
        inputHandler.handleInput(input);

        // then
        then(commandBus).should().post(any(NullCommand.class));
    }

    @Test
    void shouldHandleEmptyCommand() {
        // given
        final var input =  Input.Companion.inputOf(Bytes.emptyBytes());

        // when
        inputHandler.handleInput(input);

        // then
        then(commandBus).should().post(any(NullCommand.class));
    }

}
