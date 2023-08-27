package de.pflugradts.passbird.domain.service.password;

import com.google.inject.Inject;
import de.pflugradts.passbird.domain.model.event.PasswordEntryNotFound;
import de.pflugradts.passbird.domain.model.password.KeyAlreadyExistsException;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry;
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider;
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository;
import io.vavr.control.Try;

import static de.pflugradts.passbird.domain.service.password.PasswordService.EntryNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT;

public class RenamePasswordService implements CommonPasswordServiceCapabilities {

    @Inject
    private CryptoProvider cryptoProvider;
    @Inject
    private PasswordEntryRepository passwordEntryRepository;
    @Inject
    private EventRegistry eventRegistry;

    public Try<Void> renamePasswordEntry(final Bytes keyBytes, final Bytes newKeyBytes) {
        if (entryExists(cryptoProvider, passwordEntryRepository, eventRegistry, keyBytes, CREATE_ENTRY_NOT_EXISTS_EVENT)
                .getOrElse(false)) {
            return encrypted(cryptoProvider, newKeyBytes).fold(
                Try::failure,
                encryptedNewKeyBytes -> find(passwordEntryRepository, encryptedNewKeyBytes).isEmpty()
                    ? renamePasswordEntryOrFail(keyBytes, encryptedNewKeyBytes)
                        .andThen(() -> processEventsAndSync(eventRegistry, passwordEntryRepository))
                    : Try.failure(new KeyAlreadyExistsException(newKeyBytes))
            );
        } else {
            return Try.success(null);
        }
    }

    private Try<Void> renamePasswordEntryOrFail(final Bytes keyBytes, final Bytes newKeyBytes) {
        return encrypted(cryptoProvider, keyBytes).fold(
            Try::failure,
            encryptedKeyBytes -> Try.run(() -> find(passwordEntryRepository, encryptedKeyBytes).ifPresentOrElse(
                passwordEntry -> passwordEntry.rename(newKeyBytes),
                () -> eventRegistry.register(new PasswordEntryNotFound(encryptedKeyBytes)))));
    }

}
