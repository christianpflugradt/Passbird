package de.pflugradts.pwman3.application;

import de.pflugradts.pwman3.domain.model.password.PasswordEntry;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * AdapterPort for syncing the Password Repository with and restoring it from a physical file.
 */
public interface PasswordStoreAdapterPort {
    Supplier<Stream<PasswordEntry>> restore();
    void sync(Supplier<Stream<PasswordEntry>> passwordEntries);
}
