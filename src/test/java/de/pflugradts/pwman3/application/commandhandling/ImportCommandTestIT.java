package de.pflugradts.pwman3.application.commandhandling;

import de.pflugradts.pwman3.application.UserInterfaceAdapterPort;
import de.pflugradts.pwman3.application.UserInterfaceAdapterPortFaker;
import de.pflugradts.pwman3.application.commandhandling.command.CommandFactory;
import de.pflugradts.pwman3.application.commandhandling.handler.ImportCommandHandler;
import de.pflugradts.pwman3.application.configuration.Configuration;
import de.pflugradts.pwman3.application.configuration.ConfigurationFaker;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
import de.pflugradts.pwman3.domain.model.password.PasswordEntryFaker;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.Input;
import de.pflugradts.pwman3.application.exchange.ImportExportService;
import de.pflugradts.pwman3.domain.service.password.PasswordService;
import de.pflugradts.pwman3.domain.service.PasswordServiceFaker;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    private void setup() {
        ConfigurationFaker.faker().forInstance(configuration).fake();
        inputHandler = new InputHandler(
                new CommandBus(null, Set.of(importCommandHandler)),
                new CommandFactory());
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
        ConfigurationFaker.faker()
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
        ConfigurationFaker.faker()
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
        then(importExportService).should(never()).importPasswordEntries(args);
        assertThat(bytes).isNotEqualTo(reference);
    }

}
