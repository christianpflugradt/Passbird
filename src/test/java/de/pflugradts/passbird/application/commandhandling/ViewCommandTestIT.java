package de.pflugradts.passbird.application.commandhandling;

import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.commandhandling.handler.ViewCommandHandler;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Input;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.password.PasswordService;
import io.vavr.control.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static de.pflugradts.passbird.application.commandhandling.InputHandlerTestFactory.setupInputHandlerFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ViewCommandTestIT {

    private InputHandler inputHandler;

    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Mock
    private PasswordService passwordService;
    @InjectMocks
    private ViewCommandHandler viewCommandHandler;

    @BeforeEach
    void setup() {
        inputHandler = setupInputHandlerFor(viewCommandHandler);
    }

    @Test
    void shouldHandleViewCommand() {
        // given
        final var args = "key";
        final var bytes = Bytes.bytesOf("v" + args);
        final var reference = bytes.copy();
        final var expectedPassword = mock(Bytes.class);
        given(passwordService.viewPassword(Bytes.bytesOf(args))).willReturn(Optional.of(Try.of(() -> expectedPassword)));

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput(Input.of(bytes));

        // then
        then(userInterfaceAdapterPort).should().send(eq(Output.of(expectedPassword)));
        assertThat(bytes).isNotEqualTo(reference);
    }

}
