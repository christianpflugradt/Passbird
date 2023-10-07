package de.pflugradts.passbird.domain.service.password;

import com.google.inject.Inject;
import de.pflugradts.passbird.domain.model.Tuple;
import de.pflugradts.passbird.domain.model.password.InvalidKeyException;
import de.pflugradts.passbird.domain.model.password.PasswordEntry;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.CharValue;
import de.pflugradts.passbird.domain.service.NamespaceService;
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry;
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider;
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository;

import java.util.function.Predicate;
import java.util.stream.Stream;

class PutPasswordService implements CommonPasswordServiceCapabilities {

    @Inject
    private CryptoProvider cryptoProvider;
    @Inject
    private PasswordEntryRepository passwordEntryRepository;
    @Inject
    private NamespaceService namespaceService;
    @Inject
    private EventRegistry eventRegistry;

    public void putPasswordEntries(final Stream<Tuple<Bytes, Bytes>> passwordEntries) {
        passwordEntries.forEach(tuple -> putPasswordEntry(tuple._1, tuple._2, false));
        processEventsAndSync(eventRegistry, passwordEntryRepository);
    }

    public void putPasswordEntry(final Bytes keyBytes, final Bytes passwordBytes) {
        putPasswordEntry(keyBytes, passwordBytes, true);
    }
    public void putPasswordEntry(final Bytes keyBytes, final Bytes passwordBytes, final Boolean sync) {
        putPasswordEntryTuple(new Tuple<>(keyBytes, passwordBytes));
        if (sync) processEventsAndSync(eventRegistry, passwordEntryRepository);
    }

    private void putPasswordEntryTuple(final Tuple<Bytes, Bytes> passwordEntryTuple) {
        challengeAlias(passwordEntryTuple._1);
        final var encryptedPasswordBytes = encrypted(cryptoProvider, passwordEntryTuple._2);
        putEncryptedPasswordEntry(encrypted(cryptoProvider, passwordEntryTuple._1), encryptedPasswordBytes);
    }

    private void putEncryptedPasswordEntry(final Bytes encryptedKeyBytes, final Bytes encryptedPasswordBytes) {
        find(passwordEntryRepository, encryptedKeyBytes).ifPresentOrElse(
            passwordEntry -> passwordEntry.updatePassword(encryptedPasswordBytes),
            () -> passwordEntryRepository.add(
                PasswordEntry.create(
                    namespaceService.getCurrentNamespace().getSlot(),
                    encryptedKeyBytes,
                    encryptedPasswordBytes)
            ));
    }

    public void challengeAlias(final Bytes bytes) {
        if (CharValue.Companion.charValueOf(bytes.getByte(0)).isDigit()
                || anyMatch(bytes.copy(), b -> CharValue.Companion.charValueOf(b).isSymbol())) {
            throw new InvalidKeyException(bytes);
        }
    }

    private boolean anyMatch(final Bytes bytes, final Predicate<Byte> predicate) {
        final var result = bytes.stream().anyMatch(predicate);
        bytes.scramble();
        return result;
    }

}
