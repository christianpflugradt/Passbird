package de.pflugradts.passbird.application.exchange

import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.shell.Shell

interface ImportExportService {
    fun peekImportEggIdShells(uri: String): Map<NestSlot, List<Shell>>
    fun importEggs(uri: String)
    fun exportEggs(uri: String)
}
