package de.pflugradts.pwman3.application.commandhandling;

import de.pflugradts.pwman3.application.ClipboardAdapterPort;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.commandhandling.handler.GetCommandHandler;
import de.pflugradts.pwman3.domain.model.password.PasswordEntryFaker;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Input;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import de.pflugradts.pwman3.domain.service.PasswordServiceFaker;
import de.pflugradts.pwman3.domain.service.password.PasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.pflugradts.pwman3.application.commandhandling.InputHandlerTestFactory.setupInputHandlerFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class GetCommandTestIT {

    private InputHandler inputHandler;

    @Mock
    private PasswordService passwordService;
    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Mock
    private ClipboardAdapterPort clipboardAdapterPort;
    @InjectMocks
    private GetCommandHandler getCommandHandler;

    @BeforeEach
    private void setup() {
        inputHandler = setupInputHandlerFor(getCommandHandler);
    }

    @Test
    void shouldHandleGetCommand() {
        // given
        final var args = "key";
        final var bytes = Bytes.of("g" + args);
        final var reference = bytes.copy();
        final var expectedPassword = Bytes.of("value");
        PasswordServiceFaker.faker()
                .forInstance(passwordService)
                .withPasswordEntries(PasswordEntryFaker.faker()
                        .fakePasswordEntry()
                        .withKeyBytes(Bytes.of(args))
                        .withPasswordBytes(expectedPassword).fake()).fake();

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput(Input.of(bytes));

        // then
        then(clipboardAdapterPort).should().post(eq(Output.of(expectedPassword)));
        then(userInterfaceAdapterPort).should().send(any(Output.class));
        assertThat(bytes).isNotEqualTo(reference);
    }


    @Test
    void shouldHandleGetCommand_InvalidPasswordEntry() {
        // given
        final var args = "key";
        final var bytes = Bytes.of("g" + args);
        final var reference = bytes.copy();
        final var expectedPassword = Bytes.of("value");
        PasswordServiceFaker.faker()
            .forInstance(passwordService)
            .withPasswordEntries(PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.of("other")).fake()).fake();

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput(Input.of(bytes));

        // then
        then(clipboardAdapterPort).should(never()).post(any(Output.class));
        then(userInterfaceAdapterPort).should(never()).send(any(Output.class));
        assertThat(bytes).isNotEqualTo(reference);
    }

}
