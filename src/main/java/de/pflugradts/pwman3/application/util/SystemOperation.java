package de.pflugradts.pwman3.application.util;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import io.vavr.control.Try;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Objects;
import static de.pflugradts.pwman3.application.util.CryptoUtils.JCEKS_KEYSTORE;

public class SystemOperation {

    public boolean isConsoleAvailable() {
        return Objects.nonNull(System.console());
    }

    public Try<char[]> readPasswordFromConsole() {
        return Try.of(() -> System.console().readPassword());
    }

    public Try<Path> getPath(final String uri) {
        return Try.of(() -> Paths.get(uri));
    }

    public Try<KeyStore> getJceksInstance() {
        return Try.of(() -> KeyStore.getInstance(JCEKS_KEYSTORE));
    }

    public Try<InputStream> newInputStream(final Path path) {
        return Try.of(() -> Files.newInputStream(path));
    }

    public Try<OutputStream> newOutputStream(final Path path) {
        return Try.of(() -> Files.newOutputStream(path));
    }

    public Try<Void> writeBytesToFile(final Path path, final Bytes bytes) {
        return Try.run(() -> Files.write(path, bytes.toByteArray()));
    }

    public Try<Bytes> readBytesFromFile(final Path path) {
        return Try.of(() -> Bytes.of(Files.readAllBytes(path)));
    }

    public void exit() {
        System.exit(0);
    }

}
