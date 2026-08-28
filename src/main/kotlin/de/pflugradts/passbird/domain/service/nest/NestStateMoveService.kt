package de.pflugradts.passbird.domain.service.nest

import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.password.tree.EggRepository

class NestStateMoveService(
    private val eggRepositoryProvider: () -> EggRepository,
) : NestStateMover {
    override fun moveNest(from: Slot, to: Slot) = eggRepositoryProvider().moveNest(from, to)
}
