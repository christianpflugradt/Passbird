package de.pflugradts.passbird.domain.service.password;

import com.google.inject.Inject;
import de.pflugradts.passbird.domain.model.password.InvalidKeyException;
import de.pflugradts.passbird.domain.model.password.PasswordEntry;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.transfer.CharValue;
import de.pflugradts.passbird.domain.service.NamespaceService;
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry;
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider;
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import java.util.function.Predicate;
import java.util.stream.Collectors;
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

    public Try<Void> putPasswordEntries(final Stream<Tuple2<Bytes, Bytes>> passwordEntries) {
        final var passwordEntriesList = passwordEntries.collect(Collectors.toList());
        final var failedAliasCheck = passwordEntriesList
            .stream()
            .map(passwordEntry -> challengeAlias(passwordEntry._1))
            .filter(Try::isFailure)
            .findAny();
        return failedAliasCheck.orElseGet(() -> passwordEntriesList
            .stream()
            .map(this::putPasswordEntryTuple)
            .filter(Try::isFailure).findAny()
            .orElse(Try.success(null))
            .andThen(() -> processEventsAndSync(eventRegistry, passwordEntryRepository)));
    }

    public Try<Void> putPasswordEntry(final Bytes keyBytes, final Bytes passwordBytes) {
        return putPasswordEntryTuple(new Tuple2<>(keyBytes, passwordBytes))
            .andThen(() -> processEventsAndSync(eventRegistry, passwordEntryRepository));
    }

    private Try<Void> putPasswordEntryTuple(final Tuple2<Bytes, Bytes> passwordEntryTuple) {
        final var aliasCheck = challengeAlias(passwordEntryTuple._1);
        if (aliasCheck.isFailure()) {
            return aliasCheck;
        } else {
            final var encryptedPasswordBytes = encrypted(cryptoProvider, passwordEntryTuple._2());
            return encryptedPasswordBytes.isLeft()
                ? Try.failure(encryptedPasswordBytes.getLeft())
                : encrypted(cryptoProvider, passwordEntryTuple._1).fold(
                Try::failure,
                    encryptedKeyBytes -> putEncryptedPasswordEntry(encryptedKeyBytes, encryptedPasswordBytes.get()));
        }
    }

    private Try<Void> putEncryptedPasswordEntry(final Bytes encryptedKeyBytes, final Bytes encryptedPasswordBytes) {
        return Try.run(() -> find(passwordEntryRepository, encryptedKeyBytes).ifPresentOrElse(
            passwordEntry -> passwordEntry.updatePassword(encryptedPasswordBytes),
            () -> passwordEntryRepository.add(
                PasswordEntry.create(
                    namespaceService.getCurrentNamespace().getSlot(),
                    encryptedKeyBytes,
                    encryptedPasswordBytes)
            )));
    }

    public Try<Void> challengeAlias(final Bytes bytes) {
        return !CharValue.Companion.charValueOf(bytes.getByte(0)).isDigit()
                && noneMatch(bytes.copy(), b -> CharValue.Companion.charValueOf(b).isSymbol())
            ? Try.success(null)
            : Try.failure(new InvalidKeyException(bytes));
    }

    private boolean noneMatch(final Bytes bytes, final Predicate<Byte> predicate) {
        final var result = bytes.stream().noneMatch(predicate);
        bytes.scramble();
        return result;
    }

}
