package de.pflugradts.passbird.application.exchange

import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.slot.Slot

interface ImportExportService {
    fun peekImportEggIdShells(): TryResult<ShellMap>
    fun peekImportNests(): TryResult<List<ImportNestPreview>>
    fun importEggs()
    fun importEggs(sourceSlot: Slot, targetSlot: Slot)
    fun exportEggs()
    fun exportEggs(slots: Set<Slot>)
}

data class ImportNestPreview(val nestId: Shell, val slot: Slot, val eggIds: List<Shell>)
typealias ShellMap = Map<Slot, List<Shell>>
