package de.pflugradts.passbird.application.exchange

import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.shell.Shell

interface ImportExportService {
    fun peekImportEggIdShells(): ShellMap
    fun importEggs()
    fun exportEggs()
}

typealias ShellMap = Map<NestSlot, List<Shell>>
