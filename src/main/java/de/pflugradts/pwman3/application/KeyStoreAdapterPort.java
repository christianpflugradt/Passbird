package de.pflugradts.pwman3.application;

import de.pflugradts.pwman3.adapter.keystore.Key;
import de.pflugradts.pwman3.domain.model.transfer.Chars;
import io.vavr.control.Try;
import java.nio.file.Path;

/**
 * AdapterPort to access a KeyStore to load or store a Key used for symmetric encryption.
 */
public interface KeyStoreAdapterPort {
    Try<Key> loadKey(Chars password, Path path);
    Try<Void> storeKey(Chars password, Path path);
}
