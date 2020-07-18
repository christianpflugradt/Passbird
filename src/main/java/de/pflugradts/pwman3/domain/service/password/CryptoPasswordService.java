package de.pflugradts.pwman3.domain.service.password;

import com.google.inject.Inject;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
import de.pflugradts.pwman3.application.util.BytesComparator;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryNotFound;
import de.pflugradts.pwman3.domain.model.password.PasswordEntry;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.application.eventhandling.PwMan3EventRegistry;
import de.pflugradts.pwman3.domain.service.password.encryption.CryptoProvider;
import de.pflugradts.pwman3.domain.service.password.storage.PasswordEntryRepository;
import io.vavr.Tuple2;
import java.util.Optional;
import java.util.stream.Stream;

public class CryptoPasswordService implements PasswordService {

    @Inject
    private FailureCollector failureCollector;
    @Inject
    private CryptoProvider cryptoProvider;
    @Inject
    private PasswordEntryRepository passwordEntryRepository;
    @Inject
    private PwMan3EventRegistry pwMan3EventRegistry;

    @Override
    public boolean entryExists(final Bytes keyBytes) {
        return find(encrypted(keyBytes)).isPresent();
    }

    @Override
    public Optional<Bytes> viewPassword(final Bytes keyBytes) {
        final var encryptedKeyBytes = encrypted(keyBytes);
        final var password = find(encryptedKeyBytes)
                .map(PasswordEntry::viewPassword)
                .map(this::decrypted);
        if (password.isEmpty()) {
            pwMan3EventRegistry.register(new PasswordEntryNotFound(encryptedKeyBytes));
            pwMan3EventRegistry.processEvents();
        }
        return password;
    }

    @Override
    public void putPasswordEntries(final Stream<Tuple2<Bytes, Bytes>> passwordEntries) {
        passwordEntries.forEach(this::putPasswordEntryTuple);
        processEventsAndSync();
    }

    @Override
    public void putPasswordEntry(final Bytes keyBytes, final Bytes passwordBytes) {
        putPasswordEntryTuple(new Tuple2<>(keyBytes, passwordBytes));
        processEventsAndSync();
    }

    private void putPasswordEntryTuple(final Tuple2<Bytes, Bytes> passwordEntryTuple) {
        final var encryptedKeyBytes = encrypted(passwordEntryTuple._1());
        final var encryptedPasswordBytes = encrypted(passwordEntryTuple._2());
        find(encryptedKeyBytes).ifPresentOrElse(
            passwordEntry -> passwordEntry.updatePassword(encryptedPasswordBytes),
            () -> passwordEntryRepository.add(
                    PasswordEntry.create(encryptedKeyBytes, encryptedPasswordBytes)));

    }

    @Override
    public void discardPasswordEntry(final Bytes keyBytes) {
        final var encryptedKeyBytes = encrypted(keyBytes);
        find(encryptedKeyBytes).ifPresentOrElse(
            PasswordEntry::discard,
            () -> pwMan3EventRegistry.register(new PasswordEntryNotFound(keyBytes)));
        processEventsAndSync();
    }

    @Override
    public Stream<Bytes> findAllKeys() {
        return passwordEntryRepository
                .findAll()
                .map(PasswordEntry::viewKey)
                .map(this::decrypted)
                .sorted(new BytesComparator());
    }

    private Optional<PasswordEntry> find(final Bytes keyBytes) {
        return passwordEntryRepository.find(keyBytes);
    }

    private Bytes encrypted(final Bytes bytes) {
        return cryptoProvider
                .encrypt(bytes)
                .onFailure(throwable -> failureCollector.collectEncryptionFailure(bytes, throwable))
                .getOrElse(Bytes.empty());
    }

    private Bytes decrypted(final Bytes bytes) {
        return cryptoProvider
                .decrypt(bytes)
                .onFailure(throwable -> failureCollector.collectDecryptionFailure(bytes, throwable))
                .getOrElse(Bytes.empty());
    }

    private void processEventsAndSync() {
        pwMan3EventRegistry.processEvents();
        passwordEntryRepository.sync();
    }

}
