package de.pflugradts.pwman3.domain.service.password;

import com.google.inject.Inject;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import java.util.Optional;
import java.util.stream.Stream;

public class PasswordFacade implements PasswordService {

    @Inject
    private PutPasswordService putPasswordService;
    @Inject
    private ViewPasswordService viewPasswordService;
    @Inject
    private DiscardPasswordService discardPasswordService;
    @Inject
    private RenamePasswordService renamePasswordService;

    @Override
    public Try<Boolean> entryExists(final Bytes keyBytes, final EntryNotExistsAction entryNotExistsAction) {
        return viewPasswordService.entryExists(keyBytes, entryNotExistsAction);
    }

    @Override
    public Optional<Try<Bytes>> viewPassword(final Bytes keyBytes) {
        return viewPasswordService.viewPassword(keyBytes);
    }

    @Override
    public Try<Void> renamePasswordEntry(final Bytes keyBytes, final Bytes newKeyBytes) {
        return renamePasswordService.renamePasswordEntry(keyBytes, newKeyBytes);
    }

    @Override
    public Try<Stream<Bytes>> findAllKeys() {
        return viewPasswordService.findAllKeys();
    }

    @Override
    public Try<Void> challengeAlias(final Bytes bytes) {
        return putPasswordService.challengeAlias(bytes);
    }

    @Override
    public Try<Void> putPasswordEntries(final Stream<Tuple2<Bytes, Bytes>> passwordEntries) {
        return putPasswordService.putPasswordEntries(passwordEntries);
    }

    @Override
    public Try<Void> putPasswordEntry(final Bytes keyBytes, final Bytes passwordBytes) {
        return putPasswordService.putPasswordEntry(keyBytes, passwordBytes);
    }

    @Override
    public Try<Void> discardPasswordEntry(final Bytes keyBytes) {
        return discardPasswordService.discardPasswordEntry(keyBytes);
    }

}
