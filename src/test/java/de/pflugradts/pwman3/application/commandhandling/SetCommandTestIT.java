package de.pflugradts.pwman3.application.commandhandling;

import de.pflugradts.pwman3.application.commandhandling.command.CommandFactory;
import de.pflugradts.pwman3.application.commandhandling.handler.SetCommandHandler;
import de.pflugradts.pwman3.domain.service.PasswordProvider;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Input;
import de.pflugradts.pwman3.domain.service.PasswordService;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SetCommandTestIT {

    private InputHandler inputHandler;

    @Mock
    private PasswordService passwordService;
    @Mock
    private PasswordProvider passwordProvider;
    @InjectMocks
    private SetCommandHandler setCommandHandler;

    @BeforeEach
    private void setup() {
        inputHandler = new InputHandler(
                new CommandBus(null, Set.of(setCommandHandler)),
                new CommandFactory());
    }

    @Test
    void shouldHandleSetCommand() {
        // given
        final var args = "key";
        final var bytes = Bytes.of("s" + args);
        final var reference = bytes.copy();
        final var generatedPassword = mock(Bytes.class);
        given(passwordProvider.createNewPassword()).willReturn(generatedPassword);

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput(Input.of(bytes));

        // then
        then(passwordService).should().putPasswordEntry(eq(Bytes.of(args)), same(generatedPassword));
        assertThat(bytes).isNotEqualTo(reference);
    }

}
