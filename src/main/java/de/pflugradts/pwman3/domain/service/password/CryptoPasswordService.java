package de.pflugradts.pwman3.domain.service.password;

import com.google.inject.Inject;
import de.pflugradts.pwman3.domain.model.password.KeyAlreadyExistsException;
import de.pflugradts.pwman3.domain.service.util.AsciiUtils;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryNotFound;
import de.pflugradts.pwman3.domain.model.password.InvalidKeyException;
import de.pflugradts.pwman3.domain.model.password.PasswordEntry;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.BytesComparator;
import de.pflugradts.pwman3.domain.service.eventhandling.EventRegistry;
import de.pflugradts.pwman3.domain.service.password.encryption.CryptoProvider;
import de.pflugradts.pwman3.domain.service.password.storage.PasswordEntryRepository;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Try;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.pflugradts.pwman3.domain.service.password.PasswordService.EntryNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT;

// https://gitlab.com/christianpflugradt/pwman3/-/issues/24
@SuppressWarnings("PMD.TooManyMethods")
public class CryptoPasswordService implements PasswordService {

    @Inject
    private CryptoProvider cryptoProvider;
    @Inject
    private PasswordEntryRepository passwordEntryRepository;
    @Inject
    private EventRegistry eventRegistry;

    @Override
    public Try<Boolean> entryExists(final Bytes keyBytes, final EntryNotExistsAction entryNotExistsAction) {
        return encrypted(keyBytes).fold(Try::failure, encryptedKeyBytes -> Try.of(() ->
            checkEntryExistsOrHandleAction(encryptedKeyBytes, entryNotExistsAction)));
    }

    private boolean checkEntryExistsOrHandleAction(
        final Bytes encryptedKeyBytes, final EntryNotExistsAction entryNotExistsAction) {
        final var match = find(encryptedKeyBytes).isPresent();
        if (!match && entryNotExistsAction == CREATE_ENTRY_NOT_EXISTS_EVENT) {
            eventRegistry.register(new PasswordEntryNotFound(encryptedKeyBytes));
            eventRegistry.processEvents();
        }
        return match;
    }

    @Override
    public Optional<Try<Bytes>> viewPassword(final Bytes keyBytes) {
        return encrypted(keyBytes).fold(
            throwable -> Optional.of(Try.failure(throwable)),
            encryptedKeyBytes -> find(encryptedKeyBytes)
                .map(PasswordEntry::viewPassword)
                .map(this::decrypted)
                .map(Either::toTry).or(() -> {
                    eventRegistry.register(new PasswordEntryNotFound(keyBytes));
                    eventRegistry.processEvents();
                    return Optional.empty();
                }));
    }

    @Override
    public Try<Void> renamePasswordEntry(final Bytes keyBytes, final Bytes newKeyBytes) {
        if (entryExists(keyBytes, CREATE_ENTRY_NOT_EXISTS_EVENT).getOrElse(false)) {
            return encrypted(newKeyBytes).fold(
                Try::failure,
                encryptedNewKeyBytes -> find(encryptedNewKeyBytes).isEmpty()
                    ? renamePasswordEntryOrFail(keyBytes, encryptedNewKeyBytes).andThen(this::processEventsAndSync)
                    : Try.failure(new KeyAlreadyExistsException(newKeyBytes))
            );
        } else {
            return Try.success(null);
        }
    }

    private Try<Void> renamePasswordEntryOrFail(final Bytes keyBytes, final Bytes newKeyBytes) {
        return encrypted(keyBytes).fold(
            Try::failure,
            encryptedKeyBytes -> Try.run(() -> find(encryptedKeyBytes).ifPresentOrElse(
                passwordEntry -> passwordEntry.rename(newKeyBytes),
                () -> eventRegistry.register(new PasswordEntryNotFound(keyBytes)))));
    }

    @Override
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
            .andThen(this::processEventsAndSync));
    }

    @Override
    public Try<Void> putPasswordEntry(final Bytes keyBytes, final Bytes passwordBytes) {
        return putPasswordEntryTuple(new Tuple2<>(keyBytes, passwordBytes))
            .andThen(this::processEventsAndSync);
    }

    private Try<Void> putPasswordEntryTuple(final Tuple2<Bytes, Bytes> passwordEntryTuple) {
        final var aliasCheck = challengeAlias(passwordEntryTuple._1);
        if (aliasCheck.isFailure()) {
            return aliasCheck;
        } else {
            final var encryptedPasswordBytes = encrypted(passwordEntryTuple._2());
            return encryptedPasswordBytes.isLeft()
                ? Try.failure(encryptedPasswordBytes.getLeft())
                : encrypted(passwordEntryTuple._1).fold(
                    Try::failure, encryptedKeyBytes -> putEncryptedPasswordEntry(
                        encryptedKeyBytes, encryptedPasswordBytes.get()));
        }
    }

    @Override
    public Try<Void> challengeAlias(final Bytes bytes) {
        return !bytes.isEmpty()
            && !AsciiUtils.isDigit(bytes.getByte(0))
            && noneMatch(bytes.copy(), AsciiUtils::isSymbol)
            ? Try.success(null)
            : Try.failure(new InvalidKeyException(bytes));
    }

    private boolean noneMatch(final Bytes bytes, final Predicate<Byte> predicate) {
        final var result = bytes.stream().noneMatch(predicate);
        bytes.scramble();
        return result;
    }

    private Try<Void> putEncryptedPasswordEntry(final Bytes encryptedKeyBytes, final Bytes encryptedPasswordBytes) {
        return Try.run(() -> find(encryptedKeyBytes).ifPresentOrElse(
            passwordEntry -> passwordEntry.updatePassword(encryptedPasswordBytes),
            () -> passwordEntryRepository.add(
                PasswordEntry.create(encryptedKeyBytes, encryptedPasswordBytes))));
    }

    @Override
    public Try<Void> discardPasswordEntry(final Bytes keyBytes) {
        return discardOrFail(keyBytes).andThen(this::processEventsAndSync);
    }

    private Try<Void> discardOrFail(final Bytes keyBytes) {
        return encrypted(keyBytes).fold(
            Try::failure,
            encryptedKeyBytes -> Try.run(() -> find(encryptedKeyBytes).ifPresentOrElse(
                PasswordEntry::discard,
                () -> eventRegistry.register(new PasswordEntryNotFound(keyBytes)))));
    }

    @Override
    public Try<Stream<Bytes>> findAllKeys() {
        return getAllSortedIfNoErrors(passwordEntryRepository
            .findAll()
            .map(PasswordEntry::viewKey)
            .map(this::decrypted)
            .collect(Collectors.toList()));
    }

    private Try<Stream<Bytes>> getAllSortedIfNoErrors(final List<Either<Throwable, Bytes>> eitherList) {
        return eitherList.stream().anyMatch(Either::isLeft)
            ? Try.failure(eitherList.stream().filter(Either::isLeft).map(Either::getLeft).findAny().get())
            : Try.of(() -> eitherList.stream().map(Either::get).sorted(new BytesComparator()));
    }

    private Optional<PasswordEntry> find(final Bytes keyBytes) {
        return passwordEntryRepository.find(keyBytes);
    }

    private Either<Throwable, Bytes> encrypted(final Bytes bytes) {
        return cryptoProvider.encrypt(bytes).toEither();
    }

    private Either<Throwable, Bytes> decrypted(final Bytes bytes) {
        return cryptoProvider.decrypt(bytes).toEither();
    }

    private void processEventsAndSync() {
        eventRegistry.processEvents();
        passwordEntryRepository.sync();
    }

}
