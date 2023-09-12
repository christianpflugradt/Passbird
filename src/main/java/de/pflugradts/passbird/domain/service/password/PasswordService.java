package de.pflugradts.passbird.domain.service.password;

import de.pflugradts.passbird.domain.model.Tuple;
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;
import de.pflugradts.passbird.domain.model.transfer.Bytes;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * <p>A PasswordServices manages {@link de.pflugradts.passbird.domain.model.password.PasswordEntry PasswordEntries}.</p>
 * <p>PasswordEntries can be viewed, created, updated or removed through methods such as {@link #viewPassword(Bytes)},
 * {@link #putPasswordEntry(Bytes, Bytes)} and {@link #discardPasswordEntry(Bytes)}.</p>
 */
public interface PasswordService {

    enum EntryNotExistsAction {
        DO_NOTHING,
        CREATE_ENTRY_NOT_EXISTS_EVENT
    }
    Boolean entryExists(Bytes keyBytes, NamespaceSlot namespace);
    Boolean entryExists(Bytes keyBytes, EntryNotExistsAction entryNotExistsAction);
    Optional<Bytes> viewPassword(Bytes keyBytes);
    void renamePasswordEntry(Bytes keyBytes, Bytes newKeyBytes);
    void challengeAlias(Bytes bytes);
    void putPasswordEntries(Stream<Tuple<Bytes, Bytes>> passwordEntries);
    void putPasswordEntry(Bytes keyBytes, Bytes passwordBytes);
    void discardPasswordEntry(Bytes keyBytes);
    void movePasswordEntry(Bytes keyBytes, NamespaceSlot targetNamespace);
    Stream<Bytes> findAllKeys();
}
