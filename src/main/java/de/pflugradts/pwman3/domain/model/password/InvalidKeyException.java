package de.pflugradts.pwman3.domain.model.password;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import lombok.Getter;

public class InvalidKeyException extends RuntimeException {

    @Getter
    private final Bytes keyBytes;

    public InvalidKeyException(final Bytes keyBytes) {
        super(String.format("Key '%s' contains non alphabetic characters!", keyBytes.asString()));
        this.keyBytes = keyBytes;
    }

}
