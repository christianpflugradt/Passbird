package de.pflugradts.pwman3.adapter.passwordstore;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.pflugradts.pwman3.application.configuration.ReadableConfiguration;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
import de.pflugradts.pwman3.application.util.ByteArrayUtils;
import de.pflugradts.pwman3.application.util.SystemOperation;
import de.pflugradts.pwman3.domain.model.namespace.Namespace;
import de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot;
import de.pflugradts.pwman3.domain.model.namespace.Namespaces;
import de.pflugradts.pwman3.domain.model.password.PasswordEntry;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.service.password.encryption.CryptoProvider;
import de.pflugradts.pwman3.domain.service.password.storage.PasswordStoreAdapterPort;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static de.pflugradts.pwman3.application.configuration.ReadableConfiguration.DATABASE_FILENAME;
import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot.FIRST;
import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot.LAST;
import static java.lang.Integer.BYTES;

@NoArgsConstructor
@AllArgsConstructor
@Singleton
@SuppressWarnings({"PMD.TooManyMethods", "PMD.ExcessiveImports"})
public class PasswordFileStore implements PasswordStoreAdapterPort {

    static final Integer EMPTY_NAMESPACE = -2;
    private static final Integer EOF = 0;
    private static final Integer SECTOR = -1;

    @Inject
    private SystemOperation systemOperation;
    @Inject
    private FailureCollector failureCollector;
    @Inject
    private ReadableConfiguration configuration;
    @Inject
    private PasswordEntryTransformer passwordEntryTransformer;
    @Inject
    private NamespaceTransformer namespaceTransformer;
    @Inject
    private CryptoProvider cryptoProvider;

    @Override
    public Supplier<Stream<PasswordEntry>> restore() {
        final var passwordEntries = new ArrayDeque<PasswordEntry>();
        final var bytes = readFromDisk().onFailure(throwable ->
                        failureCollector.collectDecryptPasswordDatabaseFailure(getFilePath(), throwable));
        if (bytes.isSuccess()) {
            final var byteArray = bytes.get().toByteArray();
            if (byteArray.length > 0) {
                verifySignature(byteArray);
                verifyChecksum(byteArray);
                int offset = signatureSize();
                final var res1 = populateNamespaces(byteArray, offset);
                offset = res1._1;
                while (!EOF.equals(ByteArrayUtils.readInt(byteArray, offset))) {
                    final var res2 =
                        passwordEntryTransformer.transform(byteArray, offset, res1._2);
                    passwordEntries.add(res2._1());
                    offset = res2._2();
                }
                return passwordEntries::stream;
            }
        }
        Namespaces.populateEmpty();
        return Stream::empty;
    }

    private void verifySignature(final byte[] bytes) {
        final byte[] expectedSignature = signature();
        final byte[] actualSignature = new byte[signatureSize()];
        ByteArrayUtils.copyBytes(Bytes.of(bytes), actualSignature, 0);
        if (!Arrays.equals(expectedSignature, actualSignature)) {
            failureCollector.collectSignatureCheckFailure(Bytes.of(actualSignature));
        }
    }

    private void verifyChecksum(final byte[] bytes) {
        final var contentSize = calcActualContentSize(bytes.length);
        final var expectedChecksum = contentSize > 0
                ? checksum(Arrays.copyOfRange(bytes, signatureSize(), contentSize))
                : 0x0;
        final var actualCheckSum = bytes[bytes.length - 1];
        if (!Objects.equals(expectedChecksum, actualCheckSum)) {
            failureCollector.collectChecksumFailure(actualCheckSum, expectedChecksum);
        }
    }

    private Tuple2<Integer, Boolean> populateNamespaces(final byte[] bytes, final int offset) {
        var incrementedOffset = offset;
        var legacyMode = true;
        if (SECTOR.equals(ByteArrayUtils.readInt(bytes, incrementedOffset))) {
            incrementedOffset += intBytes();
            final List<Bytes> namespaceBytes = new ArrayList<>();
            for (int i = 0; i < NamespaceSlot.CAPACITY; i++) {
                final var result = namespaceTransformer.transform(bytes, incrementedOffset);
                namespaceBytes.add(result._1);
                incrementedOffset += result._2;
            }
            Namespaces.populate(namespaceBytes);
            legacyMode = false;
        }
        return new Tuple2<>(incrementedOffset, legacyMode);
    }

    @Override
    public void sync(final Supplier<Stream<PasswordEntry>> passwordEntriesSupplier) {
        final var contentSize = calcRequiredContentSize(passwordEntriesSupplier);
        final var bytes = new byte[calcActualTotalSize(contentSize)];
        var offset = ByteArrayUtils.copyBytes(signature(), bytes, 0, signatureSize());
        offset = persistNamespaces(bytes, offset);
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
                .onFailure(throwable ->
                        failureCollector.collectWritePasswordDatabaseFailure(getFilePath(), throwable));
    }

    private int persistNamespaces(final byte[] bytes, final int offset) {
        var incrementedOffset = offset;
        ByteArrayUtils.copyBytes(SECTOR, bytes, incrementedOffset);
        incrementedOffset += intBytes();
        for (int index = FIRST; index <= LAST; index++) {
            final var namespaceBytes = namespaceTransformer.transform(NamespaceSlot.at(index));
            incrementedOffset += ByteArrayUtils.copyBytes(
                namespaceBytes, bytes, incrementedOffset, namespaceBytes.length);
        }
        return incrementedOffset;
    }

    private Try<Bytes> readFromDisk() {
        final var bytes = systemOperation.readBytesFromFile(getFilePath());
        return bytes.isSuccess()
                ? cryptoProvider.decrypt(bytes.get())
                : Try.of(Bytes::empty);
    }

    private Try<Path> writeToDisk(final Bytes bytes) {
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
                .map(passwordEntry -> intBytes() + passwordEntry.viewKey().size() + passwordEntry.viewPassword().size())
                .reduce(0, Integer::sum);
        final var namespaceSize = intBytes() + NamespaceSlot.CAPACITY * intBytes() + Namespaces.all()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(Namespace::getBytes)
            .map(Bytes::size)
            .reduce(0, Integer::sum);
        final var metaSize = (int) passwordEntries.get().count() * 2 * intBytes();
        return dataSize + namespaceSize + metaSize;
    }

    private Path getFilePath() {
        return systemOperation.resolvePath(
                configuration.getAdapter().getPasswordStore().getLocation(),
                DATABASE_FILENAME
        ).getOrNull();
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private byte[] signature() {
        return new byte[]{0x0, 0x50, 0x77, 0x4D, 0x61, 0x6E, 0x34, 0x0};
    }

    private int signatureSize() {
        return signature().length;
    }

    private int intBytes() {
        return BYTES;
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
