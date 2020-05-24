package de.pflugradts.pwman3.domain.model.password;

import de.pflugradts.pwman3.domain.model.ddd.AggregateRoot;
import de.pflugradts.pwman3.domain.model.ddd.DomainEvent;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryCreated;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryDiscarded;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryUpdated;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(of = "key")
@ToString(of = "key")
public class PasswordEntry implements AggregateRoot {

    @Getter(AccessLevel.PRIVATE)
    private final Key key;
    @Getter(AccessLevel.PRIVATE)
    private final Password password;
    @Getter
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected PasswordEntry(final Bytes keyBytes, final Bytes passwordBytes) {
        key = Key.create(keyBytes.copy());
        password = Password.create(passwordBytes.copy());
        registerDomainEvent(new PasswordEntryCreated(this));
    }

    public static PasswordEntry create(final Bytes keyBytes, final Bytes passwordBytes) {
        return new PasswordEntry(keyBytes, passwordBytes);
    }

    public Bytes viewKey() {
        return getKey().view();
    }

    public Bytes viewPassword() {
        return getPassword().view();
    }

    public void updatePassword(final Bytes bytes) {
        getPassword().update(bytes);
        registerDomainEvent(new PasswordEntryUpdated(this));
    }

    public void discard() {
        getPassword().discard();
        registerDomainEvent(new PasswordEntryDiscarded(this));
    }

}
