package de.pflugradts.pwman3.application.commandhandling;

import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPortFaker;
import de.pflugradts.pwman3.application.commandhandling.command.CommandFactory;
import de.pflugradts.pwman3.application.commandhandling.command.namespace.NamespaceCommandFactory;
import de.pflugradts.pwman3.application.commandhandling.handler.CustomSetCommandHandler;
import de.pflugradts.pwman3.application.configuration.Configuration;
import de.pflugradts.pwman3.application.configuration.ConfigurationFaker;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
import de.pflugradts.pwman3.domain.model.password.InvalidKeyException;
import de.pflugradts.pwman3.domain.model.password.PasswordEntryFaker;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Input;
import de.pflugradts.pwman3.domain.model.transfer.Output;
import de.pflugradts.pwman3.domain.service.PasswordServiceFaker;
import de.pflugradts.pwman3.domain.service.password.PasswordService;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CustomSetCommandTestIT {

    private InputHandler inputHandler;

    @Mock
    private FailureCollector failureCollector;
    @Mock
    private PasswordService passwordService;
    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @Mock
    private Configuration configuration;
    @InjectMocks
    private CustomSetCommandHandler customSetCommandHandler;

    @BeforeEach
    private void setup() {
        ConfigurationFaker.faker().forInstance(configuration).fake();
        inputHandler = new InputHandler(
                new CommandBus(null, Set.of(customSetCommandHandler)),
                new CommandFactory(new NamespaceCommandFactory()));
    }

    @Test
    void shouldHandleCustomSetCommand() {
        // given
        final var args = "key";
        final var bytes = Bytes.of("c" + args);
        final var reference = bytes.copy();
        final var customPassword = mock(Bytes.class);
        PasswordServiceFaker.faker()
                .forInstance(passwordService).fake();
        UserInterfaceAdapterPortFaker.faker()
                .forInstance(userInterfaceAdapterPort)
                .withTheseSecureInputs(Input.of(customPassword)).fake();

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput(Input.of(bytes));

        // then
        then(passwordService).should().putPasswordEntry(eq(Bytes.of(args)), same(customPassword));
        then(customPassword).should().scramble();
        assertThat(bytes).isNotEqualTo(reference);
    }

    @Test
    void shouldHandleCustomSetCommand_WithInvalidAlias() {
        // given
        final var args = "invalidkey1!";
        final var bytes = Bytes.of("c" + args);
        final var reference = bytes.copy();
        PasswordServiceFaker.faker()
                .forInstance(passwordService)
                .withInvalidAlias(Bytes.of(args)).fake();
        ConfigurationFaker.faker()
                .forInstance(configuration)
                .withPromptOnRemovalEnabled().fake();

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput(Input.of(bytes));

        // then
        then(failureCollector).should().collectPasswordEntryFailure(eq(Bytes.of(args)), any(InvalidKeyException.class));
        then(passwordService).should(never()).putPasswordEntry(eq(Bytes.of(args)), any(Bytes.class));
        assertThat(bytes).isNotEqualTo(reference);
    }

    @Test
    void shouldHandleCustomSetCommand_WithEmptyPasswordEntered() {
        // given
        final var args = "key";
        final var bytes = Bytes.of("c" + args);
        final var reference = bytes.copy();
        final var customPassword = Bytes.empty();
        PasswordServiceFaker.faker()
                .forInstance(passwordService).fake();
        UserInterfaceAdapterPortFaker.faker()
                .forInstance(userInterfaceAdapterPort)
                .withTheseSecureInputs(Input.of(customPassword)).fake();

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput(Input.of(bytes));

        // then
        then(passwordService).should(never()).putPasswordEntry(eq(Bytes.of(args)), same(customPassword));
        then(userInterfaceAdapterPort).should().send(eq(Output.of(Bytes.of("Empty input - Operation aborted."))));
        assertThat(bytes).isNotEqualTo(reference);
    }

    @Test
    void shouldHandleCustomSetCommand_WithPromptOnRemoval_AndNewPasswordEntry() {
        // given
        final var args = "key";
        final var bytes = Bytes.of("c" + args);
        final var reference = bytes.copy();
        final var customPassword = mock(Bytes.class);
        PasswordServiceFaker.faker()
                .forInstance(passwordService).fake();
        UserInterfaceAdapterPortFaker.faker()
                .forInstance(userInterfaceAdapterPort)
                .withTheseSecureInputs(Input.of(customPassword)).fake();

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput(Input.of(bytes));

        // then
        then(passwordService).should().putPasswordEntry(eq(Bytes.of(args)), same(customPassword));
        then(customPassword).should().scramble();
        assertThat(bytes).isNotEqualTo(reference);
    }

    @Test
    void shouldHandleCustomSetCommand_WithPromptOnRemoval_AndExistingPasswordEntry() {
        // given
        final var args = "key";
        final var bytes = Bytes.of("c" + args);
        final var reference = bytes.copy();
        final var customPassword = mock(Bytes.class);
        final var givenPasswordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.of(args)).fake();
        PasswordServiceFaker.faker()
                .forInstance(passwordService)
                .withPasswordEntries(givenPasswordEntry).fake();
        ConfigurationFaker.faker()
                .forInstance(configuration)
                .withPromptOnRemovalEnabled().fake();
        UserInterfaceAdapterPortFaker.faker()
                .forInstance(userInterfaceAdapterPort)
                .withTheseSecureInputs(Input.of(customPassword))
                .withReceiveConfirmation(true).fake();

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput(Input.of(bytes));

        // then
        then(passwordService).should().putPasswordEntry(eq(Bytes.of(args)), same(customPassword));
        then(customPassword).should().scramble();
        assertThat(bytes).isNotEqualTo(reference);
    }

    @Test
    void shouldHandleCustomSetCommand_WithPromptOnRemoval_AndOperationAborted() {
        // given
        final var args = "key";
        final var bytes = Bytes.of("c" + args);
        final var reference = bytes.copy();
        final var givenPasswordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.of(args)).fake();
        PasswordServiceFaker.faker()
                .forInstance(passwordService)
                .withPasswordEntries(givenPasswordEntry).fake();
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
        then(passwordService).should(never()).putPasswordEntry(eq(Bytes.of(args)), any(Bytes.class));
        then(userInterfaceAdapterPort).should().send(eq(Output.of(Bytes.of("Operation aborted."))));
        assertThat(bytes).isNotEqualTo(reference);
    }

}
