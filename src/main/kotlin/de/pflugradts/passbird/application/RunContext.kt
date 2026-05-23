package de.pflugradts.passbird.application

import de.pflugradts.passbird.domain.model.slot.Slot

interface RunContext {
    val homeDirectory: Directory
    val initialSlot: Slot
}

data class PassbirdRunContext(
    override val homeDirectory: Directory,
    override val initialSlot: Slot,
) : RunContext
