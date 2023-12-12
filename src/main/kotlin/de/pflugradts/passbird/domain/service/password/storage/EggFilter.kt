package de.pflugradts.passbird.domain.service.password.storage

import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.nest.NestSlot
import java.util.function.Predicate

enum class EggFilter {
    CURRENT_NEST,
    ALL_NESTS,
    ;

    companion object {
        fun inNest(nestSlot: NestSlot): Predicate<Egg> = Predicate { it.associatedNest() == nestSlot }
        fun all(): Predicate<Egg> = Predicate { true }
    }
}
