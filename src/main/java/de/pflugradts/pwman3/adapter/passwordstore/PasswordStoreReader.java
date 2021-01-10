package de.pflugradts.pwman3.adapter.passwordstore;

import com.google.inject.Inject;
import de.pflugradts.pwman3.application.configuration.ReadableConfiguration;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
import de.pflugradts.pwman3.application.util.ByteArrayUtils;
import de.pflugradts.pwman3.application.util.SystemOperation;
import de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot;
import de.pflugradts.pwman3.domain.model.namespace.Namespaces;
import de.pflugradts.pwman3.domain.model.password.PasswordEntry;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.service.password.encryption.CryptoProvider;
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
import java.util.function.Supplier;
import java.util.stream.Stream;

import static de.pflugradts.pwman3.adapter.passwordstore.PasswordStoreCommons.EOF;
import static de.pflugradts.pwman3.adapter.passwordstore.PasswordStoreCommons.SECTOR;
import static de.pflugradts.pwman3.application.configuration.ReadableConfiguration.DATABASE_FILENAME;

@NoArgsConstructor
@AllArgsConstructor
class PasswordStoreReader {

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
    @Inject
    private PasswordStoreCommons commons;

    public Supplier<Stream<PasswordEntry>> restore() {
        final var passwordEntries = new ArrayDeque<PasswordEntry>();
        final var bytes = readFromDisk().onFailure(throwable ->
            failureCollector.collectDecryptPasswordDatabaseFailure(getFilePath(), throwable));
        if (bytes.isSuccess()) {
            final var byteArray = bytes.get().toByteArray();
            if (byteArray.length > 0) {
                verifySignature(byteArray);
                verifyChecksum(byteArray);
                int offset = commons.signatureSize();
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

    private Try<Bytes> readFromDisk() {
        final var bytes = systemOperation.readBytesFromFile(getFilePath());
        return bytes.isSuccess()
            ? cryptoProvider.decrypt(bytes.get())
            : Try.of(Bytes::empty);
    }

    private void verifySignature(final byte[] bytes) {
        final byte[] expectedSignature = commons.signature();
        final byte[] actualSignature = new byte[commons.signatureSize()];
        ByteArrayUtils.copyBytes(Bytes.of(bytes), actualSignature, 0);
        if (!Arrays.equals(expectedSignature, actualSignature)) {
            failureCollector.collectSignatureCheckFailure(Bytes.of(actualSignature));
        }
    }

    private void verifyChecksum(final byte[] bytes) {
        final var contentSize = calcActualContentSize(bytes.length);
        final var expectedChecksum = contentSize > 0
            ? commons.checksum(Arrays.copyOfRange(bytes, commons.signatureSize(), contentSize))
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
            incrementedOffset += commons.intBytes();
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

    private Path getFilePath() {
        return systemOperation.resolvePath(
            configuration.getAdapter().getPasswordStore().getLocation(),
            DATABASE_FILENAME
        ).getOrNull();
    }

    private int calcActualContentSize(final int totalSize) {
        return totalSize - commons.signatureSize() - commons.checksumBytes() - commons.eofBytes();
    }

}
