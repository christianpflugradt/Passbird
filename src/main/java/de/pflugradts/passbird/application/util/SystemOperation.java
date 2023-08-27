package de.pflugradts.passbird.application.util;

import de.pflugradts.passbird.domain.model.transfer.Bytes;
import io.vavr.control.Try;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Objects;

import static de.pflugradts.passbird.application.util.CryptoUtils.JCEKS_KEYSTORE;

/**
 * Wraps static methods that interact with the operating system.
 */
public class SystemOperation {

    public boolean isConsoleAvailable() {
        return Objects.nonNull(System.console());
    }

    public Try<char[]> readPasswordFromConsole() {
        return Try.of(() -> System.console().readPassword());
    }

    public Try<Path> resolvePath(final String directory, final String fileName) {
        return Try.of(() -> Paths.get(directory).resolve(fileName));
    }

    public Try<File> resolvePathToFile(final String directory, final String fileName) {
        return Try.of(() -> Paths.get(directory).resolve(fileName).toFile());
    }

    public Try<Path> getPath(final File file) {
        return Try.of(file::toPath);
    }

    public Try<Path> getPath(final String... uri) {
        return Try.of(() -> uri.length > 1
                    ? Paths.get(uri[0], Arrays.copyOfRange(uri, 1, uri.length))
                    : Paths.get(uri[0])
        );
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

    public Try<Path> writeBytesToFile(final Path path, final Bytes bytes) {
        return Try.of(() -> Files.write(path, bytes.toByteArray()));
    }

    public Try<Bytes> readBytesFromFile(final Path path) {
        return Try.of(() -> Bytes.of(Files.readAllBytes(path)));
    }

    public void copyToClipboard(final String text) {
        final var selection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }

    public Try<Bytes> getResourceAsBytes(final String resource) {
        return Try.of(() -> Bytes.of(Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resource)
                .readAllBytes()));
    }

    public Try<Void> openFile(final File file) {
        return Try.run(() -> {
            if (file.getName().endsWith(".html")) {
                getDesktop().browse(file.toURI());
            } else {
                getDesktop().open(file);
            }
        });
    }

    Desktop getDesktop() {
        return Desktop.getDesktop();
    }

    public void exit() {
        System.exit(0);
    }

}
