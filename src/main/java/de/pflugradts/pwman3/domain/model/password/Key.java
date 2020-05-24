package de.pflugradts.pwman3.domain.model.password;

import de.pflugradts.pwman3.domain.model.ddd.DomainEntity;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
final class Key implements DomainEntity {

    @Getter(AccessLevel.PRIVATE)
    private final Bytes bytes;

    private Key(final Bytes bytes) {
        this.bytes = bytes.copy();
    }

    static Key create(final Bytes bytes) {
        return new Key(bytes);
    }

    Bytes view() {
        return getBytes().copy();
    }

}
