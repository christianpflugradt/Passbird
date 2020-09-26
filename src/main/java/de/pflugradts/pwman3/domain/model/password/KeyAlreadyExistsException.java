package de.pflugradts.pwman3.domain.model.password;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import lombok.Getter;

public class KeyAlreadyExistsException extends RuntimeException {

    @Getter
    private final Bytes keyBytes;

    public KeyAlreadyExistsException(final Bytes keyBytes) {
        super(String.format("Key '%s' already exists!", keyBytes.asString()));
        this.keyBytes = keyBytes;
    }

}
