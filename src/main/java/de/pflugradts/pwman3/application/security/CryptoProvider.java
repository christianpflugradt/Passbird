package de.pflugradts.pwman3.application.security;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import io.vavr.control.Try;

public interface CryptoProvider {
    Try<Bytes> encrypt(Bytes bytes);
    Try<Bytes> decrypt(Bytes bytes);
}
