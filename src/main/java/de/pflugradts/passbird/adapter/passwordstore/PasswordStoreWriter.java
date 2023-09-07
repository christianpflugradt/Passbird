package de.pflugradts.passbird.adapter.passwordstore;

import com.google.inject.Inject;
import de.pflugradts.passbird.application.configuration.ReadableConfiguration;
import de.pflugradts.passbird.application.failurehandling.FailureCollector;
import de.pflugradts.passbird.application.util.ByteArrayUtils;
import de.pflugradts.passbird.application.util.SystemOperation;
import de.pflugradts.passbird.domain.model.namespace.Namespace;
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;
import de.pflugradts.passbird.domain.model.password.PasswordEntry;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.NamespaceService;
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider;
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

import static de.pflugradts.passbird.application.configuration.ReadableConfiguration.DATABASE_FILENAME;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.CAPACITY;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.FIRST_NAMESPACE;
import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.LAST_NAMESPACE;

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
    private NamespaceService namespaceService;
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
        writeToDisk(Bytes.bytesOf(bytes))
            .onFailure(throwable ->
                failureCollector.collectWritePasswordDatabaseFailure(getFilePath(), throwable));
    }

    private int persistNamespaces(final byte[] bytes, final int offset) {
        var incrementedOffset = offset;
        ByteArrayUtils.copyBytes(SECTOR, bytes, incrementedOffset);
        incrementedOffset += commons.intBytes();
        for (int index = FIRST_NAMESPACE; index <= LAST_NAMESPACE; index++) {
            final var namespaceBytes = namespaceTransformer.transform(NamespaceSlot.at(index));
            incrementedOffset += ByteArrayUtils.copyBytes(
                namespaceBytes, bytes, incrementedOffset, namespaceBytes.length);
        }
        return incrementedOffset;
    }

    private Try<Path> writeToDisk(final Bytes bytes) {
        return systemOperation.writeBytesToFile(
            getFilePath(),
            cryptoProvider.encrypt(bytes).getOrElse(Bytes.emptyBytes()));
    }

    private byte checksum(final byte[] bytes) {
        return (byte) StreamSupport.stream(Bytes.bytesOf(bytes).spliterator(), false)
            .mapToInt(b -> (int) (byte) b)
            .reduce(0, Integer::sum);
    }

    private int calcRequiredContentSize(final Supplier<Stream<PasswordEntry>> passwordEntries) {
        final var dataSize = passwordEntries.get()
            .map(passwordEntry ->
                commons.intBytes() + passwordEntry.viewKey().getSize() + passwordEntry.viewPassword().getSize())
            .reduce(0, Integer::sum);
        final var namespaceSize = commons.intBytes() + CAPACITY * commons.intBytes() + namespaceService.all()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(Namespace::getBytes)
            .map(Bytes::getSize)
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
