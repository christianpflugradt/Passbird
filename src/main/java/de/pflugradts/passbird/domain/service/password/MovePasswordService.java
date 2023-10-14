package de.pflugradts.passbird.domain.service.password;

import com.google.inject.Inject;
import de.pflugradts.passbird.domain.model.PasswordEntryNotFound;
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;
import de.pflugradts.passbird.domain.model.password.KeyAlreadyExistsException;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry;
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider;
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository;

public class MovePasswordService implements CommonPasswordServiceCapabilities {

    @Inject
    private CryptoProvider cryptoProvider;
    @Inject
    private PasswordEntryRepository passwordEntryRepository;
    @Inject
    private EventRegistry eventRegistry;

    public void movePassword(final Bytes keyBytes, final NamespaceSlot targetNamespace) {
        if (entryExists(cryptoProvider, passwordEntryRepository, keyBytes, targetNamespace)) {
            throw new KeyAlreadyExistsException(keyBytes);
        } else {
            var encryptedKeyBytes = encrypted(cryptoProvider, keyBytes);
            find(passwordEntryRepository, encryptedKeyBytes)
                .ifPresentOrElse(
                passwordEntry -> passwordEntry.updateNamespace(targetNamespace),
                () -> eventRegistry.register(new PasswordEntryNotFound(encryptedKeyBytes)));
            processEventsAndSync(eventRegistry, passwordEntryRepository);
        }
    }

}
