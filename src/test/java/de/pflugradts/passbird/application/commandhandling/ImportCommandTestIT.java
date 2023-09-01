package de.pflugradts.passbird.application.commandhandling;

import de.pflugradts.passbird.application.UserInterfaceAdapterPort;
import de.pflugradts.passbird.application.UserInterfaceAdapterPortFaker;
import de.pflugradts.passbird.application.commandhandling.handler.ImportCommandHandler;
import de.pflugradts.passbird.application.configuration.Configuration;
import de.pflugradts.passbird.application.configuration.MockitoConfigurationFaker;
import de.pflugradts.passbird.application.exchange.ImportExportService;
import de.pflugradts.passbird.application.failurehandling.FailureCollector;
import de.pflugradts.passbird.domain.model.password.PasswordEntryFaker;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.Input;
import de.pflugradts.passbird.domain.service.PasswordServiceFaker;
import de.pflugradts.passbird.domain.service.password.PasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static de.pflugradts.passbird.application.commandhandling.InputHandlerTestFactory.setupInputHandlerFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ImportCommandTestIT {

    private CommandBus commandBus;
    private InputHandler inputHandler;

    @Mock
    private FailureCollector failureCollector;
    @Mock
    private ImportExportService importExportService;
    @Mock
    private PasswordService passwordService;
    @Mock
    private Configuration configuration;
    @Mock
    private UserInterfaceAdapterPort userInterfaceAdapterPort;
    @InjectMocks
    private ImportCommandHandler importCommandHandler;

    @BeforeEach
    void setup() {
        MockitoConfigurationFaker.faker().forInstance(configuration).fake();
        inputHandler = setupInputHandlerFor(importCommandHandler);
    }

    @Test
    void shouldHandleImportCommand() {
        // given
        final var args = "tmp";
        final var bytes = Bytes.of("i" + args);
        final var reference = bytes.copy();

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput(Input.of(bytes));

        // then
        then(importExportService).should().importPasswordEntries(args);
        assertThat(bytes).isNotEqualTo(reference);
    }

    @Test
    void shouldHandleImportCommand_WithPromptOnRemoval_ButNoOverlappingEntries() {
        // given
        final var args = "tmp";
        final var bytes = Bytes.of("i" + args);
        final var reference = bytes.copy();
        final var importKey1 = Bytes.of("import1");
        final var importKey2 = Bytes.of("import2");
        final var databaseKey1 = Bytes.of("database1");
        final var databaseKey2 = Bytes.of("database2");
        given(importExportService.peekImportKeyBytes(args)).willReturn(Stream.of(importKey1, importKey2));
        PasswordServiceFaker.faker()
                .forInstance(passwordService)
                .withPasswordEntries(
                        PasswordEntryFaker.faker().fakePasswordEntry().withKeyBytes(databaseKey1).fake(),
                        PasswordEntryFaker.faker().fakePasswordEntry().withKeyBytes(databaseKey2).fake()
                ).fake();
        MockitoConfigurationFaker.faker()
                .forInstance(configuration)
                .withPromptOnRemovalEnabled().fake();

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput(Input.of(bytes));

        // then
        then(importExportService).should().importPasswordEntries(args);
        assertThat(bytes).isNotEqualTo(reference);
    }

    @Test
    void shouldHandleImportCommand_WithPromptOnRemoval_AndOverlappingEntries() {
        // given
        final var args = "tmp";
        final var bytes = Bytes.of("i" + args);
        final var reference = bytes.copy();
        final var importKey1 = Bytes.of("import1");
        final var importKey2 = Bytes.of("overlap");
        final var databaseKey1 = Bytes.of("database1");
        final var databaseKey2 = Bytes.of("overlap");
        given(importExportService.peekImportKeyBytes(args)).willReturn(Stream.of(importKey1, importKey2));
        PasswordServiceFaker.faker()
                .forInstance(passwordService)
                .withPasswordEntries(
                        PasswordEntryFaker.faker().fakePasswordEntry().withKeyBytes(databaseKey1).fake(),
                        PasswordEntryFaker.faker().fakePasswordEntry().withKeyBytes(databaseKey2).fake()
                ).fake();
        MockitoConfigurationFaker.faker()
                .forInstance(configuration)
                .withPromptOnRemovalEnabled().fake();
        UserInterfaceAdapterPortFaker.faker()
                .forInstance(userInterfaceAdapterPort)
                .withReceiveConfirmation(true).fake();

        // when
        assertThat(bytes).isEqualTo(reference);
        inputHandler.handleInput(Input.of(bytes));

        // then
        then(importExportService).should().importPasswordEntries(args);
        assertThat(bytes).isNotEqualTo(reference);
    }

    @Test
    void shouldHandleImportCommand_WithPromptOnRemoval_AndOperationAborted() {
        // given
        final var args = "tmp";
        final var bytes = Bytes.of("i" + args);
        final var reference = bytes.copy();
        final var importKey1 = Bytes.of("import1");
        final var importKey2 = Bytes.of("overlap");
        final var databaseKey1 = Bytes.of("database1");
        final var databaseKey2 = Bytes.of("overlap");
        given(importExportService.peekImportKeyBytes(args)).willReturn(Stream.of(importKey1, importKey2));
        PasswordServiceFaker.faker()
                .forInstance(passwordService)
                .withPasswordEntries(
                        PasswordEntryFaker.faker().fakePasswordEntry().withKeyBytes(databaseKey1).fake(),
                        PasswordEntryFaker.faker().fakePasswordEntry().withKeyBytes(databaseKey2).fake()
                ).fake();
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
        then(importExportService).should(never()).importPasswordEntries(args);
        assertThat(bytes).isNotEqualTo(reference);
    }

}
