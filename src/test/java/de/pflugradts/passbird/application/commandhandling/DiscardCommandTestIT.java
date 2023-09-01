package de.pflugradts.passbird.application.commandhandling;

import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.UserInterfaceAdapterPortFaker;
import de.pflugradts.passbird.application.commandhandling.handler.DiscardCommandHandler;
import de.pflugradts.passbird.application.configuration.Configuration;
import de.pflugradts.passbird.application.configuration.MockitoConfigurationFaker;
import de.pflugradts.passbird.application.failurehandling.FailureCollector;
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
    void setup() {
        MockitoConfigurationFaker.faker().forInstance(configuration).fake();
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
        MockitoConfigurationFaker.faker()
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
        MockitoConfigurationFaker.faker()
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
