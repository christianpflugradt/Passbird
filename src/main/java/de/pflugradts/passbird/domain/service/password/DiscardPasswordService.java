package de.pflugradts.passbird.domain.service.password;

import com.google.inject.Inject;
import de.pflugradts.passbird.domain.model.event.PasswordEntryNotFound;
import de.pflugradts.passbird.domain.model.password.PasswordEntry;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry;
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider;
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository;
import io.vavr.control.Try;

public class DiscardPasswordService implements CommonPasswordServiceCapabilities {

    @Inject
    private CryptoProvider cryptoProvider;
    @Inject
    private PasswordEntryRepository passwordEntryRepository;
    @Inject
    private EventRegistry eventRegistry;

    public Try<Void> discardPasswordEntry(final Bytes keyBytes) {
        return discardOrFail(keyBytes)
            .andThen(() -> processEventsAndSync(eventRegistry, passwordEntryRepository));
    }

    private Try<Void> discardOrFail(final Bytes keyBytes) {
        return encrypted(cryptoProvider, keyBytes).fold(
            Try::failure,
            encryptedKeyBytes -> Try.run(() -> find(passwordEntryRepository, encryptedKeyBytes).ifPresentOrElse(
                PasswordEntry::discard,
                () -> eventRegistry.register(new PasswordEntryNotFound(encryptedKeyBytes)))));
    }

}
