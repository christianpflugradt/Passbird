package de.pflugradts.passbird.application.commandhandling;

import de.pflugradts.passbird.application.ClipboardAdapterPort;
import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.commandhandling.handler.GetCommandHandler;
import de.pflugradts.passbird.domain.model.password.PasswordEntryFaker;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Input;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.PasswordServiceFaker;
import de.pflugradts.passbird.domain.service.password.PasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.pflugradts.passbird.application.commandhandling.InputHandlerTestFactory.setupInputHandlerFor;
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
    void setup() {
        inputHandler = setupInputHandlerFor(getCommandHandler);
    }

    @Test
    void shouldHandleGetCommand() {
        // given
        final var args = "key";
        final var bytes = Bytes.bytesOf("g" + args);
        final var reference = bytes.copy();
        final var expectedPassword = Bytes.bytesOf("value");
        PasswordServiceFaker.faker()
                .forInstance(passwordService)
                .withPasswordEntries(PasswordEntryFaker.faker()
                        .fakePasswordEntry()
                        .withKeyBytes(Bytes.bytesOf(args))
                        .withPasswordBytes(expectedPassword).fake()).fake();

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput(Input.of(bytes));

        // then
        then(clipboardAdapterPort).should().post(eq(Output.Companion.outputOf(expectedPassword)));
        then(userInterfaceAdapterPort).should().send(any(Output.class));
        assertThat(bytes).isNotEqualTo(reference);
    }


    @Test
    void shouldHandleGetCommand_InvalidPasswordEntry() {
        // given
        final var args = "key";
        final var bytes = Bytes.bytesOf("g" + args);
        final var reference = bytes.copy();
        final var expectedPassword = Bytes.bytesOf("value");
        PasswordServiceFaker.faker()
            .forInstance(passwordService)
            .withPasswordEntries(PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.bytesOf("other")).fake()).fake();

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput(Input.of(bytes));

        // then
        then(clipboardAdapterPort).should(never()).post(any(Output.class));
        then(userInterfaceAdapterPort).should(never()).send(any(Output.class));
        assertThat(bytes).isNotEqualTo(reference);
    }

}
