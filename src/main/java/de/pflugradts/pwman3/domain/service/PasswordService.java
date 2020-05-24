package de.pflugradts.pwman3.domain.service;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;

import java.util.Optional;
import java.util.stream.Stream;

public interface PasswordService {
    Optional<Bytes> viewPassword(Bytes keyBytes);
    void putPasswordEntry(Bytes keyBytes, Bytes passwordBytes);
    void discardPasswordEntry(Bytes keyBytes);
    Stream<Bytes> findAllKeys();
}
