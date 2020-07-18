package de.pflugradts.pwman3.domain.service.password.encryption;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import io.vavr.control.Try;

/**
 * Encrypts/Decrypts {@link Bytes} using a Key from the
 * {@link de.pflugradts.pwman3.application.KeyStoreAdapterPort KeyStore}.
 */
public interface CryptoProvider {
    Try<Bytes> encrypt(Bytes bytes);
    Try<Bytes> decrypt(Bytes bytes);
}
