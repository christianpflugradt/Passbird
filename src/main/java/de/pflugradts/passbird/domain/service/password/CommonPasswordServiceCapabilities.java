package de.pflugradts.passbird.domain.service.password;

import de.pflugradts.passbird.domain.model.PasswordEntryNotFound;
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;
import de.pflugradts.passbird.domain.model.password.PasswordEntry;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry;
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider;
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository;

import java.util.Optional;

import static de.pflugradts.passbird.domain.service.password.PasswordService.EntryNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT;

interface CommonPasswordServiceCapabilities {

    default Optional<PasswordEntry> find(final PasswordEntryRepository passwordEntryRepository,
                                         final Bytes keyBytes,
                                         final NamespaceSlot namespace) {
        return passwordEntryRepository.find(keyBytes, namespace);
    }

    default Optional<PasswordEntry> find(final PasswordEntryRepository passwordEntryRepository, final Bytes keyBytes) {
        return passwordEntryRepository.find(keyBytes);
    }

    default Bytes encrypted(final CryptoProvider cryptoProvider, final Bytes bytes) {
        return cryptoProvider.encrypt(bytes);
    }

    default Bytes decrypted(final CryptoProvider cryptoProvider, final Bytes bytes) {
        return cryptoProvider.decrypt(bytes);
    }

    default void processEventsAndSync(final EventRegistry eventRegistry,
                                      final PasswordEntryRepository passwordEntryRepository) {
        eventRegistry.processEvents();
        passwordEntryRepository.sync();
    }

    default Boolean entryExists(final CryptoProvider cryptoProvider,
                                     final PasswordEntryRepository passwordEntryRepository,
                                     final EventRegistry eventRegistry,
                                     final Bytes keyBytes,
                                     final PasswordService.EntryNotExistsAction entryNotExistsAction) {
        var encryptedKeyBytes = encrypted(cryptoProvider, keyBytes);
        final var match = find(passwordEntryRepository, encryptedKeyBytes).isPresent();
        if (!match && entryNotExistsAction == CREATE_ENTRY_NOT_EXISTS_EVENT) {
            eventRegistry.register(new PasswordEntryNotFound(encryptedKeyBytes));
            eventRegistry.processEvents();
        }
        return match;
    }

    default Boolean entryExists(final CryptoProvider cryptoProvider,
                                     final PasswordEntryRepository passwordEntryRepository,
                                     final Bytes keyBytes,
                                     final NamespaceSlot namespace) {
        return find(passwordEntryRepository, encrypted(cryptoProvider, keyBytes), namespace).isPresent();
    }

}
