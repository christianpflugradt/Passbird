package de.pflugradts.passbird.adapter.exchange

import com.fasterxml.jackson.databind.json.JsonMapper
import com.google.inject.Inject
import de.pflugradts.passbird.application.ExchangeAdapterPort
import de.pflugradts.passbird.application.Global
import de.pflugradts.passbird.application.ShellPairMap
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.EXCHANGE_FILENAME
import de.pflugradts.passbird.application.failure.ExportFailure
import de.pflugradts.passbird.application.failure.ImportFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.createNest
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.slotAt
import java.io.IOException
import java.nio.file.Files

class FilePasswordExchange @Inject constructor(
    @Inject private val systemOperation: SystemOperation,
) : ExchangeAdapterPort {
    private val mapper = JsonMapper()

    override fun send(data: ShellPairMap) {
        try {
            Files.writeString(
                systemOperation.resolvePath(Global.homeDirectory, EXCHANGE_FILENAME.toFileName()),
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ExchangeWrapper(data.toSerializable())),
            )
        } catch (e: IOException) {
            reportFailure(ExportFailure(e))
        }
    }

    override fun receive(): ShellPairMap {
        return try {
            mapper.readValue(
                Files.readString(systemOperation.resolvePath(Global.homeDirectory, EXCHANGE_FILENAME.toFileName())),
                ExchangeWrapper::class.java,
            ).exportedContent.toShellPairMap()
        } catch (e: IOException) {
            reportFailure(ImportFailure(e))
            emptyMap()
        }
    }

    private fun ShellPairMap.toSerializable() = entries.map { nest ->
        EggsPerNest(
            exportedNest = ExportedNest(nest.key.viewNestId().asString(), nest.key.slot.index()),
            exportedEggs = nest.value.map { ExportedEgg(it.first.asString(), it.second.asString()) },
        )
    }

    private fun List<EggsPerNest>.toShellPairMap() = associate { entry ->
        createNest(shellOf(entry.exportedNest.nestId), slotAt(entry.exportedNest.slot)) to (
            entry.exportedEggs.map { ShellPair(shellOf(it.eggId), shellOf(it.password)) }
            )
    }
}

private class ExportedEgg(var eggId: String = "", var password: String = "")
private class ExportedNest(var nestId: String = "", var slot: Int = 0)
private class EggsPerNest(var exportedNest: ExportedNest = ExportedNest(), var exportedEggs: List<ExportedEgg> = emptyList())
private class ExchangeWrapper(val exportedContent: List<EggsPerNest> = emptyList())
