package de.pflugradts.pwman3.domain.model.password;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;

public class InvalidKeyException extends RuntimeException {

    public InvalidKeyException(final Bytes keyBytes) {
        super(String.format("Key '%s' contains non alphabetic characters!", keyBytes.asString()));
    }

}
