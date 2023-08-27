package de.pflugradts.passbird.domain.model.password;

import de.pflugradts.passbird.domain.model.transfer.Bytes;
import de.pflugradts.passbird.domain.model.ddd.DomainEntity;
import de.pflugradts.passbird.domain.service.password.storage.PasswordStoreAdapterPort;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * A Password is stored in the {@link PasswordStoreAdapterPort PasswordStore}.
 * It is identified by a {@link Key}.
 */
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
