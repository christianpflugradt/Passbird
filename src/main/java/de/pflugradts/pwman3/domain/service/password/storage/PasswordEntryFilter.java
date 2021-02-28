package de.pflugradts.pwman3.domain.service.password.storage;

import de.pflugradts.pwman3.domain.model.namespace.NamespaceSlot;
import de.pflugradts.pwman3.domain.model.password.PasswordEntry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.function.Predicate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public enum PasswordEntryFilter {

    CURRENT_NAMESPACE,
    ALL_NAMESPACES;

    public static Predicate<PasswordEntry> inNamespace(final NamespaceSlot namespace) {
        return passwordEntry -> passwordEntry.associatedNamespace().equals(namespace);
    }

    public static Predicate<PasswordEntry> all() {
        return Objects::nonNull;
    }

}
