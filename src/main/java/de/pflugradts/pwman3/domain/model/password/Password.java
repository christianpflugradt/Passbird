package de.pflugradts.pwman3.domain.model.password;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.model.ddd.DomainEntity;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
final class Password implements DomainEntity {

    @Getter(AccessLevel.PRIVATE)
    private Bytes bytes;

    private Password(final Bytes bytes) {
        this.bytes = bytes.copy();
    }

    static Password create(final Bytes bytes) {
        return new Password(bytes);
    }

    void update(final Bytes newBytes) {
        this.bytes = newBytes.copy();
    }

    void discard() {
        this.getBytes().scramble();
    }

    Bytes view() {
        return getBytes().copy();
    }

}
