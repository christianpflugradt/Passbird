package de.pflugradts.passbird.application.exchange

import de.pflugradts.passbird.domain.model.transfer.Bytes
import java.util.stream.Stream

interface ImportExportService {
    fun peekImportKeyBytes(uri: String): Stream<Bytes>
    fun importPasswordEntries(uri: String)
    fun exportPasswordEntries(uri: String)
}
