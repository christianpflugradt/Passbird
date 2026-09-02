package de.pflugradts.passbird.application.exchange

import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.slot.Slot

interface ImportExportService {
    fun peekImportEggIdShells(password: CharArray): TryResult<ShellMap>
    fun peekImportNests(password: CharArray): TryResult<List<ImportNestPreview>>
    fun importEggs(password: CharArray)
    fun importEggs(sourceSlot: Slot, targetSlot: Slot, password: CharArray)
    fun exportEggs(password: CharArray): Boolean
    fun exportEggs(slots: Set<Slot>, password: CharArray): Boolean
}

data class ImportNestPreview(val nestId: Shell, val slot: Slot, val eggIds: List<Shell>)
typealias ShellMap = Map<Slot, List<Shell>>
