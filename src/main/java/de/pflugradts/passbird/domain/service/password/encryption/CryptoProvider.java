package de.pflugradts.passbird.domain.service.password.encryption;

import de.pflugradts.passbird.domain.model.transfer.Bytes;
import io.vavr.control.Try;

/**
 * Encrypts/Decrypts {@link Bytes} using a Key from the
 * {@link de.pflugradts.passbird.application.KeyStoreAdapterPort KeyStore}.
 */
public interface CryptoProvider {
    Try<Bytes> encrypt(Bytes bytes);
    Try<Bytes> decrypt(Bytes bytes);
}
