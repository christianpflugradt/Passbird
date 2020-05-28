package de.pflugradts.pwman3.domain.service;

import com.google.inject.Inject;
import de.pflugradts.pwman3.application.failurehandling.FailureCollector;
import de.pflugradts.pwman3.application.security.CryptoProvider;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryNotFound;
import de.pflugradts.pwman3.domain.model.password.PasswordEntry;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import java.util.Optional;
import java.util.stream.Stream;

public class PasswordRepositoryService implements PasswordService {

    @Inject
    private FailureCollector failureCollector;
    @Inject
    private CryptoProvider cryptoProvider;
    @Inject
    private PasswordEntryRepository passwordEntryRepository;
    @Inject
    private DomainEventRegistry domainEventRegistry;

    @Override
    public Optional<Bytes> viewPassword(final Bytes keyBytes) {
        final var encryptedKeyBytes = encrypted(keyBytes);
        final var password = find(encryptedKeyBytes)
                .map(PasswordEntry::viewPassword)
                .map(this::decrypted);
        if (password.isEmpty()) {
            domainEventRegistry.register(new PasswordEntryNotFound(encryptedKeyBytes));
            domainEventRegistry.processEvents();
        }
        return password;
    }

    @Override
    public void putPasswordEntry(final Bytes keyBytes, final Bytes passwordBytes) {
        final var encryptedKeyBytes = encrypted(keyBytes);
        final var encryptedPasswordBytes = encrypted(passwordBytes);
        find(encryptedKeyBytes).ifPresentOrElse(
            passwordEntry -> passwordEntry.updatePassword(encryptedPasswordBytes),
            () -> passwordEntryRepository.add(
                    PasswordEntry.create(encryptedKeyBytes, encryptedPasswordBytes)));
        passwordEntryRepository.sync();
        domainEventRegistry.processEvents();
    }

    @Override
    public void discardPasswordEntry(final Bytes keyBytes) {
        final var encryptedKeyBytes = encrypted(keyBytes);
        find(encryptedKeyBytes).ifPresentOrElse(
            PasswordEntry::discard,
            () -> domainEventRegistry.register(new PasswordEntryNotFound(keyBytes)));
        domainEventRegistry.processEvents();
    }

    @Override
    public Stream<Bytes> findAllKeys() {
        return passwordEntryRepository
                .findAll()
                .map(PasswordEntry::viewKey)
                .map(this::decrypted);
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

}
