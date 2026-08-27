package de.pflugradts.passbird.domain.service.nest

import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.slot.Slot

interface NestStateView {
    fun currentNestSlot(): Slot
    fun snapshot(): List<Shell>
    fun moveToNestAt(slot: Slot)
}
