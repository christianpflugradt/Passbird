package de.pflugradts.passbird.domain.model.password;

import de.pflugradts.passbird.domain.model.transfer.Bytes;

public class InvalidKeyException extends RuntimeException {

    private final Bytes keyBytes;

    public InvalidKeyException(final Bytes keyBytes) {
        super(String.format("Key '%s' contains non alphabetic characters!", keyBytes.asString()));
        this.keyBytes = keyBytes;
    }

    public Bytes getKeyBytes() {
        return keyBytes;
    }
}
