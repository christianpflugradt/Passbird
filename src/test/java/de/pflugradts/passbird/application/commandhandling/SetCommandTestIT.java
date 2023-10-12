package de.pflugradts.passbird.application.commandhandling;

import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.UserInterfaceAdapterPortFaker;
import de.pflugradts.passbird.application.commandhandling.handler.SetCommandHandler;
import de.pflugradts.passbird.application.configuration.Configuration;
import de.pflugradts.passbird.application.configuration.MockitoConfigurationFaker;
import de.pflugradts.passbird.domain.model.password.PasswordEntryFaker;
import de.pflugradts.passbird.domain.model.password.PasswordRequirements;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Input;
import de.pflugradts.passbird.domain.model.transfer.Output;
import de.pflugradts.passbird.domain.service.PasswordProviderFaker;
import de.pflugradts.passbird.domain.service.PasswordServiceFaker;
import de.pflugradts.passbird.domain.service.password.PasswordService;
import de.pflugradts.passbird.domain.service.password.provider.PasswordProvider;
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
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SetCommandTestIT {

    private InputHandler inputHandler;

    @Mock
    private PasswordService passwordService;
    @Mock
    private PasswordProvider passwordProvider;
    @Mock
    private Configuration configuration;
    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @InjectMocks
    private SetCommandHandler setCommandHandler;

    @BeforeEach
    void setup() {
        MockitoConfigurationFaker.faker().forInstance(configuration).fake();
        inputHandler = setupInputHandlerFor(setCommandHandler);
    }

    @Test
    void shouldHandleSetCommand() {
        // given
        final var args = "key";
        final var bytes = Bytes.bytesOf("s" + args);
        final var reference = bytes.copy();
        final var generatedPassword = Bytes.bytesOf("p4s5w0rD");
        PasswordProviderFaker.faker()
                .forInstance(passwordProvider)
                .withCreatingThisPassword(generatedPassword).fake();
        PasswordServiceFaker.faker()
                .forInstance(passwordService).fake();
        given(configuration.parsePasswordRequirements()).willReturn(mock(PasswordRequirements.class));

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput(Input.Companion.inputOf(bytes));

        // then
        then(passwordService).should().putPasswordEntry(eq(Bytes.bytesOf(args)), same(generatedPassword));
        assertThat(bytes).isNotEqualTo(reference);
    }

    @Test
    void shouldHandleSetCommand_WithInvalidAlias() {
        // given
        final var args = "invalidkey1!";
        final var bytes = Bytes.bytesOf("s" + args);
        final var reference = bytes.copy();
        PasswordServiceFaker.faker()
            .forInstance(passwordService)
            .withInvalidAlias(Bytes.bytesOf(args)).fake();
        UserInterfaceAdapterPortFaker.faker()
            .forInstance(userInterfaceAdapterPort).fake();

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput( Input.Companion.inputOf(bytes));

        // then
        then(userInterfaceAdapterPort).should().send(eq(Output.Companion.outputOf(Bytes.bytesOf("Password alias cannot contain digits or special characters. Please choose a different alias."))));
        then(passwordService).should(never()).putPasswordEntry(eq(Bytes.bytesOf(args)), any(Bytes.class));
        assertThat(bytes).isNotEqualTo(reference);
    }

    @Test
    void shouldHandleSetCommand_WithPromptOnRemoval_AndNewPasswordEntry() {
        // given
        final var args = "key";
        final var bytes = Bytes.bytesOf("s" + args);
        final var reference = bytes.copy();
        final var generatedPassword = Bytes.bytesOf("p4s5w0rD");
        PasswordProviderFaker.faker()
                .forInstance(passwordProvider)
                .withCreatingThisPassword(generatedPassword).fake();
        PasswordServiceFaker.faker()
                .forInstance(passwordService).fake();
        MockitoConfigurationFaker.faker()
                .forInstance(configuration)
                .withPromptOnRemovalEnabled().fake();
        given(configuration.parsePasswordRequirements()).willReturn(mock(PasswordRequirements.class));

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput( Input.Companion.inputOf(bytes));

        // then
        then(passwordService).should().putPasswordEntry(eq(Bytes.bytesOf(args)), same(generatedPassword));
        assertThat(bytes).isNotEqualTo(reference);
    }

    @Test
    void shouldHandleSetCommand_WithPromptOnRemoval_AndExistingPasswordEntry() {
        // given
        final var args = "key";
        final var bytes = Bytes.bytesOf("s" + args);
        final var reference = bytes.copy();
        final var generatedPassword = Bytes.bytesOf("p4s5w0rD");
        final var givenPasswordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.bytesOf(args)).fake();
        PasswordProviderFaker.faker()
                .forInstance(passwordProvider)
                .withCreatingThisPassword(generatedPassword).fake();
        PasswordServiceFaker.faker()
                .forInstance(passwordService)
                .withPasswordEntries(givenPasswordEntry).fake();
        MockitoConfigurationFaker.faker()
                .forInstance(configuration)
                .withPromptOnRemovalEnabled().fake();
        UserInterfaceAdapterPortFaker.faker()
                .forInstance(userInterfaceAdapterPort)
                .withReceiveConfirmation(true).fake();
        given(configuration.parsePasswordRequirements()).willReturn(mock(PasswordRequirements.class));

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput( Input.Companion.inputOf(bytes));

        // then
        then(passwordService).should().putPasswordEntry(eq(Bytes.bytesOf(args)), same(generatedPassword));
        assertThat(bytes).isNotEqualTo(reference);
    }

    @Test
    void shouldHandleSetCommand_WithPromptOnRemoval_AndOperationAborted() {
        // given
        final var args = "key";
        final var bytes = Bytes.bytesOf("s" + args);
        final var reference = bytes.copy();
        final var givenPasswordEntry = PasswordEntryFaker.faker()
                .fakePasswordEntry()
                .withKeyBytes(Bytes.bytesOf(args)).fake();
        PasswordServiceFaker.faker()
                .forInstance(passwordService)
                .withPasswordEntries(givenPasswordEntry).fake();
        MockitoConfigurationFaker.faker()
                .forInstance(configuration)
                .withPromptOnRemovalEnabled().fake();
        UserInterfaceAdapterPortFaker.faker()
                .forInstance(userInterfaceAdapterPort)
                .withReceiveConfirmation(false).fake();

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput( Input.Companion.inputOf(bytes));

        // then
        then(passwordService).should(never()).putPasswordEntry(eq(Bytes.bytesOf(args)), any(Bytes.class));
        then(userInterfaceAdapterPort).should().send(eq(Output.Companion.outputOf(Bytes.bytesOf("Operation aborted."))));
        assertThat(bytes).isNotEqualTo(reference);
    }

}
