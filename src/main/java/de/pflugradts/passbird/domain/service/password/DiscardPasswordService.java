package de.pflugradts.passbird.domain.service.password;

import com.google.inject.Inject;
import de.pflugradts.passbird.domain.model.PasswordEntryNotFound;
import de.pflugradts.passbird.domain.model.password.PasswordEntry;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry;
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider;
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository;

public class DiscardPasswordService implements CommonPasswordServiceCapabilities {

    @Inject
    private CryptoProvider cryptoProvider;
    @Inject
    private PasswordEntryRepository passwordEntryRepository;
    @Inject
    private EventRegistry eventRegistry;

    public void discardPasswordEntry(final Bytes keyBytes) {
        discardOrFail(keyBytes);
        processEventsAndSync(eventRegistry, passwordEntryRepository);
    }

    private void discardOrFail(final Bytes keyBytes) {
        var encryptedKeyBytes = encrypted(cryptoProvider, keyBytes);
        find(passwordEntryRepository, encryptedKeyBytes).ifPresentOrElse(
            PasswordEntry::discard,
            () -> eventRegistry.register(new PasswordEntryNotFound(encryptedKeyBytes)));
    }

}
