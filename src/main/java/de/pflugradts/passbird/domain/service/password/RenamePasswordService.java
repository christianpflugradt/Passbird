package de.pflugradts.passbird.domain.service.password;

import com.google.inject.Inject;
import de.pflugradts.passbird.domain.model.PasswordEntryNotFound;
import de.pflugradts.passbird.domain.model.password.KeyAlreadyExistsException;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry;
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider;
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository;

import static de.pflugradts.passbird.domain.service.password.PasswordService.EntryNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT;

public class RenamePasswordService implements CommonPasswordServiceCapabilities {

    @Inject
    private CryptoProvider cryptoProvider;
    @Inject
    private PasswordEntryRepository passwordEntryRepository;
    @Inject
    private EventRegistry eventRegistry;

    public void renamePasswordEntry(final Bytes keyBytes, final Bytes newKeyBytes) {
        if (entryExists(cryptoProvider, passwordEntryRepository, eventRegistry, keyBytes, CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            var encryptedNewKeyBytes = encrypted(cryptoProvider, newKeyBytes);
            if (find(passwordEntryRepository, encryptedNewKeyBytes).isEmpty()) {
                renamePasswordEntryOrFail(keyBytes, encryptedNewKeyBytes);
                processEventsAndSync(eventRegistry, passwordEntryRepository);
            } else {
                throw new KeyAlreadyExistsException(newKeyBytes);
            }
        }
    }

    private void renamePasswordEntryOrFail(final Bytes keyBytes, final Bytes newKeyBytes) {
        var encryptedKeyBytes = encrypted(cryptoProvider, keyBytes);
        find(passwordEntryRepository, encryptedKeyBytes).ifPresentOrElse(
                passwordEntry -> passwordEntry.rename(newKeyBytes),
                () -> eventRegistry.register(new PasswordEntryNotFound(encryptedKeyBytes)));
    }

}
