package de.pflugradts.pwman3.application.failurehandling;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FailureHandlingTestIT {

    private FailureCollector failureCollector;
    private ByteArrayOutputStream outputStream;
    private PrintStream printStream;

    @BeforeEach
    private void setup() {
        failureCollector = new FailureCollector(null, new FailureHandler());
        outputStream = new ByteArrayOutputStream();
        printStream = new PrintStream(outputStream);
        System.setErr(printStream);
    }

    @AfterEach
    private void cleanup() throws IOException {
        printStream.close();
        outputStream.close();
    }

    @Test
    void shouldHandleChecksumFailure() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();

        // when
        failureCollector.acceptChecksumFailure(Byte.valueOf("0"), Byte.valueOf("0"));
        final var actual = new String(outputStream.toByteArray());

        // then
        assertThat(actual).isNotNull().containsIgnoringCase("checksum");
    }

    @Test
    void shouldHandleDecryptionFailure() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();

        // when
        failureCollector.acceptDecryptionFailure(Bytes.empty(), new RuntimeException());
        final var actual = new String(outputStream.toByteArray());

        // then
        assertThat(actual).isNotNull().containsIgnoringCase("decrypted");
    }

    @Test
    void shouldHandleEncryptionFailure() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();

        // when
        failureCollector.acceptEncryptionFailure(Bytes.empty(), new RuntimeException());
        final var actual = new String(outputStream.toByteArray());

        // then
        assertThat(actual).isNotNull().containsIgnoringCase("encrypted");
    }

    @Test
    void shouldHandleExportFailure() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();

        // when
        failureCollector.acceptExportFailure(new RuntimeException());
        final var actual = new String(outputStream.toByteArray());

        // then
        assertThat(actual).isNotNull().containsIgnoringCase("exported");
    }

    @Test
    void shouldHandleImportFailure() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();

        // when
        failureCollector.acceptImportFailure(new RuntimeException());
        final var actual = new String(outputStream.toByteArray());

        // then
        assertThat(actual).isNotNull().containsIgnoringCase("imported");
    }

    @Test
    void shouldHandleInputFailure() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();

        // when
        failureCollector.acceptInputFailure(new RuntimeException());
        final var actual = new String(outputStream.toByteArray());

        // then
        assertThat(actual).isNotNull().containsIgnoringCase("input");
    }

    @Test
    void shouldHandleReadPasswordDatabaseFailure() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();

        // when
        failureCollector.acceptReadPasswordDatabaseFailure(mock(Path.class), new RuntimeException());
        final var actual = new String(outputStream.toByteArray());

        // then
        assertThat(actual).isNotNull().containsIgnoringCase("database");
    }

    @Test
    void shouldHandleSignatureCheckFailure() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();

        // when
        failureCollector.acceptSignatureCheckFailure(Bytes.empty());
        final var actual = new String(outputStream.toByteArray());

        // then
        assertThat(actual).isNotNull().containsIgnoringCase("signature");
    }

    @Test
    void shouldHandleWritePasswordDatabaseFailure() {
        // given
        assertThat(outputStream.toByteArray()).isEmpty();

        // when
        failureCollector.acceptWritePasswordDatabaseFailure(mock(Path.class), new RuntimeException());
        final var actual = new String(outputStream.toByteArray());

        // then
        assertThat(actual).isNotNull().containsIgnoringCase("database");
    }

}
