package de.pflugradts.passbird.domain.service.password;

import com.google.inject.Inject;
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
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
    @Inject
    private MovePasswordService movePasswordService;

    @Override
    public Boolean entryExists(final Bytes keyBytes, final NamespaceSlot namespace) {
        return viewPasswordService.entryExists(keyBytes, namespace);
    }

    @Override
    public Boolean entryExists(final Bytes keyBytes, final EntryNotExistsAction entryNotExistsAction) {
        return viewPasswordService.entryExists(keyBytes, entryNotExistsAction);
    }

    @Override
    public Optional<Bytes> viewPassword(final Bytes keyBytes) {
        return viewPasswordService.viewPassword(keyBytes);
    }

    @Override
    public void renamePasswordEntry(final Bytes keyBytes, final Bytes newKeyBytes) {
        renamePasswordService.renamePasswordEntry(keyBytes, newKeyBytes);
    }

    @Override
    public Stream<Bytes> findAllKeys() {
        return viewPasswordService.findAllKeys();
    }

    @Override
    public void challengeAlias(final Bytes bytes) {
        putPasswordService.challengeAlias(bytes);
    }

    @Override
    public void putPasswordEntries(final Stream<Tuple2<Bytes, Bytes>> passwordEntries) {
        putPasswordService.putPasswordEntries(passwordEntries);
    }

    @Override
    public void putPasswordEntry(final Bytes keyBytes, final Bytes passwordBytes) {
        putPasswordService.putPasswordEntry(keyBytes, passwordBytes);
    }

    @Override
    public void discardPasswordEntry(final Bytes keyBytes) {
        discardPasswordService.discardPasswordEntry(keyBytes);
    }

    @Override
    public void movePasswordEntry(final Bytes keyBytes, final NamespaceSlot targetNamespace) {
        movePasswordService.movePassword(keyBytes, targetNamespace);
    }

}
