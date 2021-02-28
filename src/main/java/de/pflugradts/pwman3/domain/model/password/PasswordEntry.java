package de.pflugradts.pwman3.domain.model.password;

import de.pflugradts.pwman3.domain.model.ddd.AggregateRoot;
import de.pflugradts.pwman3.domain.model.ddd.DomainEvent;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryCreated;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryDiscarded;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryRenamed;
import de.pflugradts.pwman3.domain.model.event.PasswordEntryUpdated;
import de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot;
import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import de.pflugradts.pwman3.domain.service.password.storage.PasswordStoreAdapterPort;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * A PasswordEntry represents a {@link Key} and an associated {@link Password} stored in the
 * {@link PasswordStoreAdapterPort PasswordStore}.
 */
@EqualsAndHashCode(of = {"key", "namespace"})
@ToString(of = {"key", "namespace"})
public class PasswordEntry implements AggregateRoot {

    @Getter(AccessLevel.PRIVATE)
    private final Key key;
    @Getter(AccessLevel.PRIVATE)
    private final Password password;
    @Getter(AccessLevel.PRIVATE)
    private NamespaceSlot namespace;
    @Getter
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected PasswordEntry(final NamespaceSlot namespaceSlot, final Bytes keyBytes, final Bytes passwordBytes) {
        namespace = namespaceSlot;
        key = Key.create(keyBytes.copy());
        password = Password.create(passwordBytes.copy());
        registerDomainEvent(new PasswordEntryCreated(this));
    }

    public static PasswordEntry create(final NamespaceSlot namespaceSlot,
                                       final Bytes keyBytes,
                                       final Bytes passwordBytes) {
        return new PasswordEntry(namespaceSlot, keyBytes, passwordBytes);
    }

    public NamespaceSlot associatedNamespace() {
        return namespace;
    }

    public Bytes viewKey() {
        return getKey().view();
    }

    public Bytes viewPassword() {
        return getPassword().view();
    }

    public void rename(final Bytes keyBytes) {
        getKey().rename(keyBytes);
        registerDomainEvent(new PasswordEntryRenamed(this));
    }

    public void updatePassword(final Bytes bytes) {
        getPassword().update(bytes);
        registerDomainEvent(new PasswordEntryUpdated(this));
    }

    public void updateNamespace(final NamespaceSlot namespaceSlot) {
        this.namespace = namespaceSlot;
    }

    public void discard() {
        getPassword().discard();
        registerDomainEvent(new PasswordEntryDiscarded(this));
    }

}
