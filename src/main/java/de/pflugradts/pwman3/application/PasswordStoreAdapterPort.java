package de.pflugradts.pwman3.application;

import de.pflugradts.pwman3.domain.model.password.PasswordEntry;

import java.util.function.Supplier;
import java.util.stream.Stream;

public interface PasswordStoreAdapterPort {
    Supplier<Stream<PasswordEntry>> restore();
    void sync(Supplier<Stream<PasswordEntry>> passwordEntries);
}
