package de.pflugradts.passbird.application.exchange

import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.transfer.Bytes

interface ImportExportService {
    fun peekImportKeyBytes(uri: String): Map<NamespaceSlot, List<Bytes>>
    fun importPasswordEntries(uri: String)
    fun exportPasswordEntries(uri: String)
}
