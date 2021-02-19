package de.pflugradts.pwman3.application.commandhandling;

import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPortFaker;
import de.pflugradts.pwman3.application.commandhandling.handler.DiscardCommandHandler;
import de.pflugradts.pwman3.application.configuration.Configuration;
import de.pflugradts.pwman3.application.configuration.ConfigurationFaker;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DiscardCommandTestIT {

    private InputHandler inputHandler;

    @Mock
    private FailureCollector failureCollector;
    @Mock
    private PasswordService passwordService;
    @Mock
    private Configuration configuration;
    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @InjectMocks
    private DiscardCommandHandler discardCommandHandler;

    @BeforeEach
    private void setup() {
        ConfigurationFaker.faker().forInstance(configuration).fake();
        inputHandler = setupInputHandlerFor(discardCommandHandler);
    }

    @Test
    void shouldHandleDiscardCommand() {
        // given
        final var args = "key";
        final var bytes = Bytes.of("d" + args);
        final var reference = bytes.copy();
        PasswordServiceFaker.faker()
                .forInstance(passwordService)
                .withPasswordEntries(PasswordEntryFaker.faker()
                        .fakePasswordEntry()
                        .withKeyBytes(Bytes.of(args)).fake()).fake();

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput(Input.of(bytes));

        // then
        then(passwordService).should().discardPasswordEntry(eq(Bytes.of(args)));
        assertThat(bytes).isNotEqualTo(reference);
    }

    @Test
    void shouldHandleDiscardCommand_WithPromptOnRemoval() {
        // given
        final var args = "key";
        final var bytes = Bytes.of("d" + args);
        final var reference = bytes.copy();
        ConfigurationFaker.faker()
                .forInstance(configuration)
                .withPromptOnRemovalEnabled().fake();
        UserInterfaceAdapterPortFaker.faker()
                .forInstance(userInterfaceAdapterPort)
                .withReceiveConfirmation(true).fake();
        PasswordServiceFaker.faker()
                .forInstance(passwordService)
                .withPasswordEntries(PasswordEntryFaker.faker()
                        .fakePasswordEntry()
                        .withKeyBytes(Bytes.of(args)).fake()).fake();

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput(Input.of(bytes));

        // then
        then(passwordService).should().discardPasswordEntry(eq(Bytes.of(args)));
        assertThat(bytes).isNotEqualTo(reference);
    }

    @Test
    void shouldHandleDiscardCommand_WithPromptOnRemoval_AndOperationAborted() {
        // given
        final var args = "key";
        final var bytes = Bytes.of("d" + args);
        final var reference = bytes.copy();
        ConfigurationFaker.faker()
                .forInstance(configuration)
                .withPromptOnRemovalEnabled().fake();
        UserInterfaceAdapterPortFaker.faker()
                .forInstance(userInterfaceAdapterPort)
                .withReceiveConfirmation(false).fake();

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput(Input.of(bytes));

        // then
        then(passwordService).should(never()).discardPasswordEntry(eq(Bytes.of(args)));
        then(userInterfaceAdapterPort).should().send(eq(Output.of(Bytes.of("Operation aborted."))));
        assertThat(bytes).isNotEqualTo(reference);
    }

}
