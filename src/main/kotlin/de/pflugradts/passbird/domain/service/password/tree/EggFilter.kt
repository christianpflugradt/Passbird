package de.pflugradts.passbird.domain.service.password.tree

import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.slot.Slot
import java.util.function.Predicate

enum class EggFilter {
    CURRENT_NEST,
    ALL_NESTS,
    ;

    companion object {
        fun inNest(slot: Slot): Predicate<Egg> = Predicate { it.associatedNest() == slot }
        fun all(): Predicate<Egg> = Predicate { true }
    }
}
