package de.pflugradts.passbird.domain.service.password.storage

import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.password.PasswordEntry
import java.util.function.Predicate

enum class PasswordEntryFilter {
    CURRENT_NAMESPACE,
    ALL_NAMESPACES,
    ;

    companion object {
        fun inNamespace(namespace: NamespaceSlot): Predicate<PasswordEntry> = Predicate { it.associatedNamespace() == namespace }
        fun all(): Predicate<PasswordEntry> = Predicate { true }
    }
}
