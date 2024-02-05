package de.pflugradts.passbird.application.exchange

import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.slot.Slot

interface ImportExportService {
    fun peekImportEggIdShells(): ShellMap
    fun importEggs()
    fun exportEggs()
}

typealias ShellMap = Map<Slot, List<Shell>>
