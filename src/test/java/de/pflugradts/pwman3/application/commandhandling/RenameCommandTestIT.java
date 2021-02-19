package de.pflugradts.pwman3.application.commandhandling;

import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPortFaker;
import de.pflugradts.pwman3.application.commandhandling.handler.RenameCommandHandler;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class RenameCommandTestIT {

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
    private RenameCommandHandler renameCommandHandler;

    @BeforeEach
    private void setup() {
        ConfigurationFaker.faker().forInstance(configuration).fake();
        inputHandler = setupInputHandlerFor(renameCommandHandler);
    }

    @Test
    void shouldHandleRenameCommand() {
        // given
        final var args = "key123";
        final var bytes = Bytes.of("r" + args);
        final var reference = bytes.copy();
        final var newKey = mock(Bytes.class);
        final var givenPasswordEntry = PasswordEntryFaker.faker()
            .fakePasswordEntry()
            .withKeyBytes(Bytes.of(args)).fake();
        PasswordServiceFaker.faker()
                .forInstance(passwordService)
                .withPasswordEntries(givenPasswordEntry).fake();
        UserInterfaceAdapterPortFaker.faker()
                .forInstance(userInterfaceAdapterPort)
                .withTheseInputs(Input.of(newKey)).fake();

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput(Input.of(bytes));

        // then
        then(passwordService).should().renamePasswordEntry(eq(Bytes.of(args)), same(newKey));
        then(newKey).should().scramble();
        assertThat(givenPasswordEntry.viewKey()).isEqualTo(reference.slice(1, reference.size()));
        assertThat(bytes).isNotEqualTo(reference);
    }

    @Test
    void shouldHandleRenameCommand_WithUnknownAlias() {
        // given
        final var args = "invalidkey1!";
        final var bytes = Bytes.of("r" + args);
        final var reference = bytes.copy();
        PasswordServiceFaker.faker()
                .forInstance(passwordService)
                .withInvalidAlias(Bytes.of(args)).fake();

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput(Input.of(bytes));

        // then
        then(passwordService).should(never()).renamePasswordEntry(eq(Bytes.of(args)), any(Bytes.class));
        assertThat(bytes).isNotEqualTo(reference);
    }

    @Test
    void shouldHandleRenameCommand_WithEmptyAliasEntered() {
        // given
        final var args = "key123";
        final var bytes = Bytes.of("r" + args);
        final var reference = bytes.copy();
        final var newAlias = Bytes.of("");
        PasswordServiceFaker.faker()
                .forInstance(passwordService)
                .withPasswordEntries(PasswordEntryFaker.faker()
                        .fakePasswordEntry()
                        .withKeyBytes(Bytes.of(args)).fake()).fake();
        UserInterfaceAdapterPortFaker.faker()
                .forInstance(userInterfaceAdapterPort)
                .withTheseInputs(Input.of(newAlias)).fake();

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput(Input.of(bytes));

        // then
        then(passwordService).should(never()).renamePasswordEntry(eq(Bytes.of(args)), any(Bytes.class));
        then(userInterfaceAdapterPort).should().send(eq(Output.of(Bytes.of("Empty input - Operation aborted."))));
        assertThat(bytes).isNotEqualTo(reference);
    }

    @Test
    void shouldHandleRenameCommand_WithExistingAliasEntered() {
        // given
        final var args = "key123";
        final var bytes = Bytes.of("r" + args);
        final var reference = bytes.copy();
        final var existingAlias = Bytes.of("existingkey");
        final var givenPasswordEntry = PasswordEntryFaker.faker()
            .fakePasswordEntry()
            .withKeyBytes(Bytes.of(args)).fake();
        PasswordServiceFaker.faker()
                .forInstance(passwordService)
                .withPasswordEntries(givenPasswordEntry).fake();
        UserInterfaceAdapterPortFaker.faker()
                .forInstance(userInterfaceAdapterPort)
                .withTheseInputs(Input.of(existingAlias)).fake();

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput(Input.of(bytes));

        // then
        assertThat(givenPasswordEntry.viewKey()).isEqualTo(reference.slice(1, reference.size()));
        assertThat(bytes).isNotEqualTo(reference);
    }

}
