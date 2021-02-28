package de.pflugradts.pwman3.domain.service.password;

import de.pflugradts.pwman3.domain.model.event.PasswordEntryNotFound;
import de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot;
import de.pflugradts.pwman3.domain.model.password.PasswordEntry;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.service.eventhandling.EventRegistry;
import de.pflugradts.pwman3.domain.service.password.encryption.CryptoProvider;
import de.pflugradts.pwman3.domain.service.password.storage.PasswordEntryRepository;
import io.vavr.control.Either;
import io.vavr.control.Try;

import java.util.Optional;

import static de.pflugradts.pwman3.domain.service.password.PasswordService.EntryNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT;

interface CommonPasswordServiceCapabilities {

    default Optional<PasswordEntry> find(final PasswordEntryRepository passwordEntryRepository,
                                         final Bytes keyBytes,
                                         final NamespaceSlot namespace) {
        return passwordEntryRepository.find(keyBytes, namespace);
    }

    default Optional<PasswordEntry> find(final PasswordEntryRepository passwordEntryRepository, final Bytes keyBytes) {
        return passwordEntryRepository.find(keyBytes);
    }

    default Either<Throwable, Bytes> encrypted(final CryptoProvider cryptoProvider, final Bytes bytes) {
        return cryptoProvider.encrypt(bytes).toEither();
    }

    default Either<Throwable, Bytes> decrypted(final CryptoProvider cryptoProvider, final Bytes bytes) {
        return cryptoProvider.decrypt(bytes).toEither();
    }

    default void processEventsAndSync(final EventRegistry eventRegistry,
                                      final PasswordEntryRepository passwordEntryRepository) {
        eventRegistry.processEvents();
        passwordEntryRepository.sync();
    }

    default Try<Boolean> entryExists(final CryptoProvider cryptoProvider,
                                     final PasswordEntryRepository passwordEntryRepository,
                                     final EventRegistry eventRegistry,
                                     final Bytes keyBytes,
                                     final PasswordService.EntryNotExistsAction entryNotExistsAction) {
        return encrypted(cryptoProvider, keyBytes).fold(Try::failure, encryptedKeyBytes -> Try.of(() -> {
            final var match = find(passwordEntryRepository, encryptedKeyBytes).isPresent();
            if (!match && entryNotExistsAction == CREATE_ENTRY_NOT_EXISTS_EVENT) {
                eventRegistry.register(new PasswordEntryNotFound(encryptedKeyBytes));
                eventRegistry.processEvents();
            }
            return match;
        }));
    }

    default Try<Boolean> entryExists(final CryptoProvider cryptoProvider,
                                     final PasswordEntryRepository passwordEntryRepository,
                                     final Bytes keyBytes,
                                     final NamespaceSlot namespace) {
        return encrypted(cryptoProvider, keyBytes).fold(Try::failure, encryptedKeyBytes -> Try.of(() ->
            find(passwordEntryRepository, encryptedKeyBytes, namespace).isPresent()));
    }

}
