package de.pflugradts.passbird.application;

import de.pflugradts.passbird.application.security.Key;
import de.pflugradts.passbird.domain.model.transfer.Chars;
import io.vavr.control.Try;
import java.nio.file.Path;

/**
 * AdapterPort to access a KeyStore to load or store a Key used for symmetric encryption.
 */
public interface KeyStoreAdapterPort {
    Try<Key> loadKey(Chars password, Path path);
    Try<Void> storeKey(Chars password, Path path);
}
