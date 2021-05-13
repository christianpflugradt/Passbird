package de.pflugradts.pwman3.domain.service.password;

import com.google.inject.Inject;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryNotFound;
import de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot;
import de.pflugradts.pwman3.domain.model.password.PasswordEntry;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.transfer.BytesComparator;
import de.pflugradts.pwman3.domain.service.eventhandling.EventRegistry;
import de.pflugradts.pwman3.domain.service.password.encryption.CryptoProvider;
import de.pflugradts.pwman3.domain.service.password.storage.PasswordEntryRepository;
import io.vavr.control.Either;
import io.vavr.control.Try;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ViewPasswordService implements CommonPasswordServiceCapabilities {

    @Inject
    private CryptoProvider cryptoProvider;
    @Inject
    private PasswordEntryRepository passwordEntryRepository;
    @Inject
    private EventRegistry eventRegistry;

    public Try<Boolean> entryExists(final Bytes keyBytes, final NamespaceSlot namespace) {
        return entryExists(cryptoProvider, passwordEntryRepository, keyBytes, namespace);
    }

    public Try<Boolean> entryExists(final Bytes keyBytes,
                                    final PasswordService.EntryNotExistsAction entryNotExistsAction) {
        return entryExists(cryptoProvider, passwordEntryRepository, eventRegistry, keyBytes, entryNotExistsAction);
    }

    public Optional<Try<Bytes>> viewPassword(final Bytes keyBytes) {
        return encrypted(cryptoProvider, keyBytes).fold(
            throwable -> Optional.of(Try.failure(throwable)),
            encryptedKeyBytes -> find(passwordEntryRepository, encryptedKeyBytes)
                .map(PasswordEntry::viewPassword)
                .map(passwordBytes -> decrypted(cryptoProvider, passwordBytes))
                .map(Either::toTry).or(() -> {
                    eventRegistry.register(new PasswordEntryNotFound(encryptedKeyBytes));
                    eventRegistry.processEvents();
                    return Optional.empty();
                }));
    }

    public Try<Stream<Bytes>> findAllKeys() {
        return getAllSortedIfNoErrors(passwordEntryRepository
            .findAll()
            .map(PasswordEntry::viewKey)
            .map(passwordBytes -> decrypted(cryptoProvider, passwordBytes))
            .collect(Collectors.toList()));
    }

    private Try<Stream<Bytes>> getAllSortedIfNoErrors(final List<Either<Throwable, Bytes>> eitherList) {
        return eitherList.stream().anyMatch(Either::isLeft)
            ? Try.failure(eitherList.stream().filter(Either::isLeft).map(Either::getLeft).findAny().get())
            : Try.of(() -> eitherList.stream().map(Either::get).sorted(new BytesComparator()));
    }

}
