package de.pflugradts.passbird.domain.model.password;

import de.pflugradts.passbird.domain.model.transfer.Bytes;
import lombok.Getter;

public class InvalidKeyException extends RuntimeException {

    @Getter
    private final Bytes keyBytes;

    public InvalidKeyException(final Bytes keyBytes) {
        super(String.format("Key '%s' contains non alphabetic characters!", keyBytes.asString()));
        this.keyBytes = keyBytes;
    }

}
