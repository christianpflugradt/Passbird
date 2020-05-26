package de.pflugradts.pwman3.domain.service;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * <p>A PasswordServices manages {@link de.pflugradts.pwman3.domain.model.password.PasswordEntry PasswordEntries}.</p>
 * <p>PasswordEntries can be viewed, created, updated or removed through methods such as {@link #viewPassword(Bytes)},
 * {@link #putPasswordEntry(Bytes, Bytes)} and {@link #discardPasswordEntry(Bytes)}.</p>
 */
public interface PasswordService {
    Optional<Bytes> viewPassword(Bytes keyBytes);
    void putPasswordEntry(Bytes keyBytes, Bytes passwordBytes);
    void discardPasswordEntry(Bytes keyBytes);
    Stream<Bytes> findAllKeys();
}
