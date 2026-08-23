package de.pflugradts.passbird.domain.service.password

import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.slot.Slot

data class ProteinEntry(
    val slot: Slot,
    val typeShell: Shell,
    val structureShell: Shell,
)
