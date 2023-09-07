package de.pflugradts.passbird.application.commandhandling;

import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.boot.Bootable;
import de.pflugradts.passbird.application.commandhandling.handler.QuitCommandHandler;
import de.pflugradts.passbird.application.util.SystemOperation;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Input;
import de.pflugradts.passbird.domain.model.transfer.Output;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.pflugradts.passbird.application.commandhandling.InputHandlerTestFactory.setupInputHandlerFor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class QuitCommandTestIT {

    private InputHandler inputHandler;

    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Mock
    private Bootable bootable;
    @InjectMocks
    private QuitCommandHandler quitCommandHandler;

    @BeforeEach
    void setup() {
        inputHandler = setupInputHandlerFor(quitCommandHandler);
    }

    @Test
    void shouldHandleQuitCommand() {
        // given
        final var input =  Input.Companion.inputOf(Bytes.bytesOf("q"));

        // when
        inputHandler.handleInput(input);

        // then
        then(userInterfaceAdapterPort).should().send(any(Output.class));
        then(bootable).should().terminate(any(SystemOperation.class));
    }

}
