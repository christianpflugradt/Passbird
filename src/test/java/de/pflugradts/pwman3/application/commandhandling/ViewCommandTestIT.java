package de.pflugradts.pwman3.application.commandhandling;

import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.commandhandling.command.CommandFactory;
import de.pflugradts.pwman3.application.commandhandling.command.namespace.NamespaceCommandFactory;
import de.pflugradts.pwman3.application.commandhandling.handler.ViewCommandHandler;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Input;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import de.pflugradts.pwman3.domain.service.password.PasswordService;
import io.vavr.control.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

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
    private void setup() {
        inputHandler = new InputHandler(
                new CommandBus(null, Set.of(viewCommandHandler)),
                new CommandFactory(new NamespaceCommandFactory()));
    }

    @Test
    void shouldHandleViewCommand() {
        // given
        final var args = "key";
        final var bytes = Bytes.of("v" + args);
        final var reference = bytes.copy();
        final var expectedPassword = mock(Bytes.class);
        given(passwordService.viewPassword(Bytes.of(args))).willReturn(Optional.of(Try.of(() -> expectedPassword)));

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput(Input.of(bytes));

        // then
        then(userInterfaceAdapterPort).should().send(eq(Output.of(expectedPassword)));
        assertThat(bytes).isNotEqualTo(reference);
    }

}
