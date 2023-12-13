package de.pflugradts.passbird.application.exchange

import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.shell.Shell

interface ImportExportService {
    fun peekImportEggIdShells(uri: String): ShellMap
    fun importEggs(uri: String)
    fun exportEggs(uri: String)
}

typealias ShellMap = Map<NestSlot, List<Shell>>
