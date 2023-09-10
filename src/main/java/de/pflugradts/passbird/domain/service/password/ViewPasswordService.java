package de.pflugradts.passbird.domain.service.password;

import com.google.inject.Inject;
import de.pflugradts.passbird.domain.model.event.PasswordEntryNotFound;
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;
import de.pflugradts.passbird.domain.model.password.PasswordEntry;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.BytesComparator;
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry;
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider;
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ViewPasswordService implements CommonPasswordServiceCapabilities {

    @Inject
    private CryptoProvider cryptoProvider;
    @Inject
    private PasswordEntryRepository passwordEntryRepository;
    @Inject
    private EventRegistry eventRegistry;

    public Boolean entryExists(final Bytes keyBytes, final NamespaceSlot namespace) {
        return entryExists(cryptoProvider, passwordEntryRepository, keyBytes, namespace);
    }

    public Boolean entryExists(final Bytes keyBytes,
                                    final PasswordService.EntryNotExistsAction entryNotExistsAction) {
        return entryExists(cryptoProvider, passwordEntryRepository, eventRegistry, keyBytes, entryNotExistsAction);
    }

    public Optional<Bytes> viewPassword(final Bytes keyBytes) {
        var encryptedKeyBytes = encrypted(cryptoProvider, keyBytes);
        return find(passwordEntryRepository, encryptedKeyBytes)
            .map(PasswordEntry::viewPassword)
            .map(passwordBytes -> decrypted(cryptoProvider, passwordBytes))
            .or(() -> {
                eventRegistry.register(new PasswordEntryNotFound(encryptedKeyBytes));
                eventRegistry.processEvents();
                return Optional.empty();
            });
    }

    public Stream<Bytes> findAllKeys() {
        return getAllSortedIfNoErrors(passwordEntryRepository
            .findAll()
            .map(PasswordEntry::viewKey)
            .map(passwordBytes -> decrypted(cryptoProvider, passwordBytes))
            .collect(Collectors.toList()));
    }

    private Stream<Bytes> getAllSortedIfNoErrors(final List<Bytes> bytesList) {
        return bytesList.stream().sorted(new BytesComparator());
    }

}
