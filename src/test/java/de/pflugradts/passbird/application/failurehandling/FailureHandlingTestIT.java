package de.pflugradts.passbird.application.failurehandling;

import de.pflugradts.passbird.application.boot.Bootable;
import de.pflugradts.passbird.application.configuration.Configuration;
import de.pflugradts.passbird.application.configuration.MockitoConfigurationFaker;
import de.pflugradts.passbird.application.util.SystemOperation;
import de.pflugradts.passbird.domain.model.password.InvalidKeyException;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class FailureHandlingTestIT {

    @Mock
    private Bootable bootable;
    @Mock
    private Configuration configuration;
    @Mock
    private SystemOperation systemOperation;
    @InjectMocks
    private FailureHandler failureHandler;

    private FailureCollector failureCollector;
    private ByteArrayOutputStream outputStream;
    private PrintStream printStream;

    @BeforeEach
    void setup() {
        failureCollector = new FailureCollector(null, failureHandler);
        outputStream = new ByteArrayOutputStream();
        printStream = new PrintStream(outputStream);
        System.setErr(printStream);
    }

    @AfterEach
    void cleanup() throws IOException {
        printStream.close();
        outputStream.close();
    }

    @Test
    void shouldHandleChecksumFailure_Strict() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();
        MockitoConfigurationFaker.faker()
                .forInstance(configuration)
                .withVerifyChecksumEnabled().fake();

        // when
        failureCollector.collectChecksumFailure(Byte.valueOf("0"), Byte.valueOf("0"));
        final var actual = outputStream.toString();

        // then
        assertThat(actual).isNotNull().containsIgnoringCase("checksum");
        then(bootable).should().terminate(systemOperation);
    }

    @Test
    void shouldHandleChecksumFailure_Lenient() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();
        MockitoConfigurationFaker.faker().forInstance(configuration).fake();

        // when
        failureCollector.collectChecksumFailure(Byte.valueOf("0"), Byte.valueOf("0"));
        final var actual = outputStream.toString();

        // then
        assertThat(actual).isNotNull().containsIgnoringCase("checksum");
        then(bootable).should(never()).terminate(systemOperation);
    }

    @Test
    void shouldHandleCommandFailure() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();
        final var expectedMessage = "print this error to stderr";

        // when
        failureCollector.collectCommandFailure(new RuntimeException(expectedMessage));
        final var actual = outputStream.toString();

        // then
        assertThat(actual).isNotNull().isEqualTo(expectedMessage + System.lineSeparator());
    }

    @Test
    void shouldHandleRenamePasswordEntryFailure() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();

        // when
        failureCollector.collectRenamePasswordEntryFailure(new RuntimeException());
        final var actual = outputStream.toString();

        // then
        assertThat(actual).isNotNull().containsIgnoringCase("could not be renamed");
    }

    @Test
    void shouldHandlePasswordEntryFailure() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();

        // when
        failureCollector.collectPasswordEntryFailure(Bytes.emptyBytes(), new RuntimeException());
        final var actual = outputStream.toString();

        // then
        assertThat(actual).isNotNull().containsIgnoringCase("password entry");
    }

    @Test
    void shouldHandlePasswordEntryFailure_InvalidKeyException() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();

        // when
        failureCollector.collectPasswordEntryFailure(Bytes.emptyBytes(), new InvalidKeyException(Bytes.emptyBytes()));
        final var actual = outputStream.toString();

        // then
        assertThat(actual).isNotNull().containsIgnoringCase("password alias");
    }

    @Test
    void shouldHandlePasswordEntriesFailure() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();

        // when
        failureCollector.collectPasswordEntriesFailure(new RuntimeException());
        final var actual = outputStream.toString();

        // then
        assertThat(actual).isNotNull().containsIgnoringCase("password entries");
    }

    @Test
    void shouldHandleExportFailure() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();

        // when
        failureCollector.collectExportFailure(new RuntimeException());
        final var actual = outputStream.toString();

        // then
        assertThat(actual).isNotNull().containsIgnoringCase("exported");
    }

    @Test
    void shouldHandleImportFailure() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();

        // when
        failureCollector.collectImportFailure(new RuntimeException());
        final var actual = outputStream.toString();

        // then
        assertThat(actual).isNotNull().containsIgnoringCase("imported");
    }

    @Test
    void shouldHandleImportFailure_InvalidKeyException() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();
        final var keystring = "mykey";

        // when
        failureCollector.collectImportFailure(new InvalidKeyException(Bytes.bytesOf(keystring)));
        final var actual = outputStream.toString();

        // then
        assertThat(actual).isNotNull().containsIgnoringCase(keystring);
    }

    @Test
    void shouldHandleInputFailure() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();

        // when
        failureCollector.collectInputFailure(new RuntimeException());
        final var actual = outputStream.toString();

        // then
        assertThat(actual).isNotNull().containsIgnoringCase("input");
    }

    @Test
    void shouldHandleDecryptPasswordDatabaseFailure() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();
        MockitoConfigurationFaker.faker().forInstance(configuration).fake();
        given(systemOperation.resolvePath(any(), any())).willReturn(mock(Path.class));

        // when
        failureCollector.collectDecryptPasswordDatabaseFailure(mock(Path.class), new RuntimeException());
        final var actual = outputStream.toString();

        // then
        assertThat(actual).isNotNull().containsIgnoringCase("database");
        then(bootable).should().terminate(systemOperation);
    }

    @Test
    void shouldHandleSignatureCheckFailure_Strict() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();
        MockitoConfigurationFaker.faker()
                .forInstance(configuration)
                .withVerifySignatureEnabled().fake();

        // when
        failureCollector.collectSignatureCheckFailure(Bytes.emptyBytes());
        final var actual = outputStream.toString();

        // then
        assertThat(actual).isNotNull().containsIgnoringCase("signature");
        then(bootable).should().terminate(systemOperation);
    }

    @Test
    void shouldHandleSignatureCheckFailure_Lenient() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();
        MockitoConfigurationFaker.faker().forInstance(configuration).fake();

        // when
        failureCollector.collectSignatureCheckFailure(Bytes.emptyBytes());
        final var actual = outputStream.toString();

        // then
        assertThat(actual).isNotNull().containsIgnoringCase("signature");
        then(bootable).should(never()).terminate(systemOperation);
    }

    @Test
    void shouldHandleWritePasswordDatabaseFailure() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();

        // when
        failureCollector.collectWritePasswordDatabaseFailure(mock(Path.class), new RuntimeException());
        final var actual = outputStream.toString();

        // then
        assertThat(actual).isNotNull().containsIgnoringCase("database");
    }

    @Test
    void shouldHandleClipboardFailure() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();
        final var expectedMessage = "print this error to stderr";

        // when
        failureCollector.collectClipboardFailure(new RuntimeException(expectedMessage));
        final var actual = outputStream.toString();

        // then
        assertThat(actual).isNotNull().contains(expectedMessage + System.lineSeparator());
    }

}
