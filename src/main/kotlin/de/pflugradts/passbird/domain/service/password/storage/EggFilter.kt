package de.pflugradts.passbird.domain.service.password.storage

import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.nest.Slot
import java.util.function.Predicate

enum class EggFilter {
    CURRENT_NEST,
    ALL_NESTS,
    ;

    companion object {
        fun inNest(nestSlot: Slot): Predicate<Egg> = Predicate { it.associatedNest() == nestSlot }
        fun all(): Predicate<Egg> = Predicate { true }
    }
}
