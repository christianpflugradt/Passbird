package de.pflugradts.passbird.domain.service.nest

import de.pflugradts.passbird.domain.model.slot.Slot

interface NestStateMover {
    fun moveNest(from: Slot, to: Slot)
}
