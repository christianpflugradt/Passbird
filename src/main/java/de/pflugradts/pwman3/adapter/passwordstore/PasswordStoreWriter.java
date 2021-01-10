package de.pflugradts.pwman3.adapter.passwordstore;

import com.google.inject.Inject;
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
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static de.pflugradts.pwman3.application.configuration.ReadableConfiguration.DATABASE_FILENAME;
import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot.FIRST;
import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot.LAST;

@NoArgsConstructor
@AllArgsConstructor
class PasswordStoreWriter {

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
    @Inject
    private PasswordStoreCommons commons;

    public void sync(final Supplier<Stream<PasswordEntry>> passwordEntriesSupplier) {
        final var contentSize = calcRequiredContentSize(passwordEntriesSupplier);
        final var bytes = new byte[calcActualTotalSize(contentSize)];
        var offset = ByteArrayUtils.copyBytes(commons.signature(), bytes, 0, commons.signatureSize());
        offset = persistNamespaces(bytes, offset);
        for (final PasswordEntry passwordEntry : passwordEntriesSupplier.get().collect(Collectors.toList())) {
            final var entryBytes = passwordEntryTransformer.transform(passwordEntry);
            offset += ByteArrayUtils.copyBytes(entryBytes, bytes, offset, entryBytes.length);
        }
        offset += ByteArrayUtils.copyBytes(EOF, bytes, offset);
        final byte[] checksumBytes = {contentSize > 0
                ? checksum(Arrays.copyOfRange(bytes, commons.signatureSize(), contentSize))
                : 0x0};
        ByteArrayUtils.copyBytes(checksumBytes, bytes, offset, commons.checksumBytes());
        writeToDisk(Bytes.of(bytes))
            .onFailure(throwable ->
                failureCollector.collectWritePasswordDatabaseFailure(getFilePath(), throwable));
    }

    private int persistNamespaces(final byte[] bytes, final int offset) {
        var incrementedOffset = offset;
        ByteArrayUtils.copyBytes(SECTOR, bytes, incrementedOffset);
        incrementedOffset += commons.intBytes();
        for (int index = FIRST; index <= LAST; index++) {
            final var namespaceBytes = namespaceTransformer.transform(NamespaceSlot.at(index));
            incrementedOffset += ByteArrayUtils.copyBytes(
                namespaceBytes, bytes, incrementedOffset, namespaceBytes.length);
        }
        return incrementedOffset;
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
            .map(passwordEntry ->
                commons.intBytes() + passwordEntry.viewKey().size() + passwordEntry.viewPassword().size())
            .reduce(0, Integer::sum);
        final var namespaceSize = commons.intBytes() + NamespaceSlot.CAPACITY * commons.intBytes() + Namespaces.all()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(Namespace::getBytes)
            .map(Bytes::size)
            .reduce(0, Integer::sum);
        final var metaSize = (int) passwordEntries.get().count() * 2 * commons.intBytes();
        return dataSize + namespaceSize + metaSize;
    }

    private Path getFilePath() {
        return systemOperation.resolvePath(
            configuration.getAdapter().getPasswordStore().getLocation(),
            DATABASE_FILENAME
        ).getOrNull();
    }

    private int calcActualTotalSize(final int contentSize) {
        return commons.signatureSize() + contentSize + commons.eofBytes() + commons.checksumBytes();
    }

}
