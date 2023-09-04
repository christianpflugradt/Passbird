package de.pflugradts.passbird.domain.model.password;

import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot;
import de.pflugradts.passbird.domain.model.transfer.Bytes;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import static de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.DEFAULT;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordEntryFaker {

    private Bytes password;
    private Bytes key;
    private NamespaceSlot namespace;

    public static PasswordEntryFaker faker() {
        return new PasswordEntryFaker();
    }

    public PasswordEntryFaker fakePasswordEntry() {
        namespace = DEFAULT;
        key = Bytes.bytesOf("key");
        password = Bytes.bytesOf("password");
        return this;
    }

    public PasswordEntryFaker withNamespace(final NamespaceSlot namespaceSlot) {
        namespace = namespaceSlot;
        return this;
    }

    public PasswordEntryFaker withKeyBytes(final Bytes keyBytes) {
        key = keyBytes;
        return this;
    }

    public PasswordEntryFaker withPasswordBytes(final Bytes passwordBytes) {
        password = passwordBytes;
        return this;
    }

    public PasswordEntry fake() {
        return PasswordEntry.create(namespace, key, password);
    }

}
