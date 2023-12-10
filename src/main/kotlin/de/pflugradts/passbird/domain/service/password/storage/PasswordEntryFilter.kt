package de.pflugradts.passbird.domain.service.password.storage

import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.password.PasswordEntry
import java.util.function.Predicate

enum class PasswordEntryFilter {
    CURRENT_NEST,
    ALL_NESTS,
    ;

    companion object {
        fun inNest(nestSlot: Slot): Predicate<PasswordEntry> = Predicate { it.associatedNest() == nestSlot }
        fun all(): Predicate<PasswordEntry> = Predicate { true }
    }
}
