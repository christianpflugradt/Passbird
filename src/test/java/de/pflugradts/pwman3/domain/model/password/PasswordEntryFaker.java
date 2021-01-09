package de.pflugradts.pwman3.domain.model.password;

import de.pflugradts.pwman3.domain.model.transfer.Bytes;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import static de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot.DEFAULT;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PasswordEntryFaker {

    private Bytes password;
    private Bytes key;

    public static PasswordEntryFaker faker() {
        return new PasswordEntryFaker();
    }

    public PasswordEntryFaker fakePasswordEntry() {
        key = Bytes.of("key");
        password = Bytes.of("password");
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
        return PasswordEntry.create(DEFAULT, key, password);
    }

}
