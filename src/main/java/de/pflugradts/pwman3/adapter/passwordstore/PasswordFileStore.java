package de.pflugradts.pwman3.adapter.passwordstore;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.pflugradts.pwman3.application.PasswordStoreAdapterPort;
import de.pflugradts.pwman3.application.configuration.ReadableConfiguration;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
import de.pflugradts.pwman3.application.security.CryptoProvider;
import de.pflugradts.pwman3.application.util.ByteArrayUtils;
import de.pflugradts.pwman3.application.util.SystemOperation;
import de.pflugradts.pwman3.domain.model.password.PasswordEntry;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import static de.pflugradts.pwman3.application.configuration.ReadableConfiguration.DATABASE_FILENAME;

@AllArgsConstructor
@NoArgsConstructor
@Singleton
@SuppressWarnings("PMD.TooManyMethods")
public class PasswordFileStore implements PasswordStoreAdapterPort {

    private static final Integer EOF = 0;

    @Inject
    private SystemOperation systemOperation;
    @Inject
    private FailureCollector failureCollector;
    @Inject
    private ReadableConfiguration configuration;
    @Inject
    private PasswordEntryTransformer passwordEntryTransformer;
    @Inject
    private CryptoProvider cryptoProvider;

    @Override
    public Supplier<Stream<PasswordEntry>> restore() {
        final var passwordEntries = new ArrayDeque<PasswordEntry>();
        final var bytes = readFromDisk()
                .onFailure(throwable -> failureCollector.acceptReadPasswordDatabaseFailure(getFilePath(), throwable));
        if (bytes.isSuccess()) {
            final var byteArray = bytes.get().toByteArray();
            if (byteArray.length > 0) {
                verifySignature(byteArray);
                verifyChecksum(byteArray);
                int offset = signatureSize();
                while (!EOF.equals(ByteArrayUtils.readInt(byteArray, offset))) {
                    final Tuple2<PasswordEntry, Integer> result = passwordEntryTransformer.transform(byteArray, offset);
                    passwordEntries.add(result._1);
                    offset = result._2;
                }
                return passwordEntries::stream;
            }
        }
        return Stream::empty;
    }

    private void verifySignature(final byte[] bytes) {
        final byte[] expectedSignature = signature();
        final byte[] actualSignature = new byte[signatureSize()];
        ByteArrayUtils.copyBytes(Bytes.of(bytes), actualSignature, 0);
        if (!Arrays.equals(expectedSignature, actualSignature)) {
            failureCollector.acceptSignatureCheckFailure(Bytes.of(actualSignature));
        }
    }

    private void verifyChecksum(final byte[] bytes) {
        final var contentSize = calcActualContentSize(bytes.length);
        final var expectedChecksum = contentSize > 0
                ? checksum(Arrays.copyOfRange(bytes, signatureSize(), contentSize))
                : 0x0;
        final var actualCheckSum = bytes[bytes.length - 1];
        if (!Objects.equals(expectedChecksum, actualCheckSum)) {
            failureCollector.acceptChecksumFailure(actualCheckSum, expectedChecksum);
        }
    }

    @Override
    public void sync(final Supplier<Stream<PasswordEntry>> passwordEntriesSupplier) {
        final var contentSize = calcRequiredContentSize(passwordEntriesSupplier);
        final var bytes = new byte[calcActualTotalSize(contentSize)];
        var offset = ByteArrayUtils.copyBytes(signature(), bytes, 0, signatureSize());
        for (final PasswordEntry passwordEntry : passwordEntriesSupplier.get().collect(Collectors.toList())) {
            final var entryBytes = passwordEntryTransformer.transform(passwordEntry);
            offset += ByteArrayUtils.copyBytes(entryBytes, bytes, offset, entryBytes.length);
        }
        offset += ByteArrayUtils.copyBytes(EOF, bytes, offset);
        final byte[] checksumBytes = {contentSize > 0
                        ? checksum(Arrays.copyOfRange(bytes, signatureSize(), contentSize))
                        : 0x0};
        ByteArrayUtils.copyBytes(checksumBytes, bytes, offset, checksumBytes());
        writeToDisk(Bytes.of(bytes))
                .onFailure(throwable -> failureCollector.acceptWritePasswordDatabaseFailure(getFilePath(), throwable));
    }

    private Try<Bytes> readFromDisk() {
        final var bytes = systemOperation.readBytesFromFile(getFilePath());
        return bytes.isSuccess()
                ? cryptoProvider.decrypt(bytes.get())
                : bytes;
    }

    private Try<Void> writeToDisk(final Bytes bytes) {
        return systemOperation.writeBytesToFile(
                getFilePath(),
                cryptoProvider.encrypt(bytes).getOrElse(Bytes.empty()));
    }

    private byte checksum(final byte[] bytes) {
        return (byte) StreamSupport.stream(Bytes.of(bytes).spliterator(), false)
                .mapToInt(b -> (int) (byte) b)
                .reduce(0, Integer::sum);
    }

    private int calcRequiredContentSize(final Supplier<Stream<PasswordEntry>> passwordEntries) {
        final var dataSize = passwordEntries.get()
                .map(passwordEntry -> passwordEntry.viewKey().size() + passwordEntry.viewPassword().size())
                .reduce(0, Integer::sum);
        final var metaSize = (int) passwordEntries.get().count() * 2 * intBytes();
        return dataSize + metaSize;
    }

    private Path getFilePath() {
        return Paths.get(configuration.getAdapter().getPasswordStore().getLocation()).resolve(DATABASE_FILENAME);
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private byte[] signature() {
        return new byte[]{0x0, 0x50, 0x77, 0x4D, 0x61, 0x6E, 0x33, 0x0};
    }

    private int signatureSize() {
        return signature().length;
    }

    private int intBytes() {
        return Integer.BYTES;
    }

    private int eofBytes() {
        return intBytes();
    }

    private int checksumBytes() {
        return 1;
    }

    private int calcActualContentSize(final int totalSize) {
        return totalSize - signatureSize() - checksumBytes() - eofBytes();
    }

    private int calcActualTotalSize(final int contentSize) {
        return signatureSize() + contentSize + eofBytes() + checksumBytes();
    }

}
