package de.pflugradts.passbird.adapter.exchange

import com.fasterxml.jackson.databind.json.JsonMapper
import com.google.inject.Inject
import de.pflugradts.passbird.application.ExchangeAdapterPort
import de.pflugradts.passbird.application.Global
import de.pflugradts.passbird.application.PasswordInfo
import de.pflugradts.passbird.application.PasswordInfoMap
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
    private val systemOperation: SystemOperation,
) : ExchangeAdapterPort {
    private val mapper = JsonMapper()

    override fun send(data: PasswordInfoMap) {
        try {
            Files.writeString(
                systemOperation.resolvePath(Global.homeDirectory, EXCHANGE_FILENAME.toFileName()),
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ExchangeWrapper(data.toSerializable())),
            )
        } catch (e: IOException) {
            reportFailure(ExportFailure(e))
        }
    }

    override fun receive() = try {
        mapper.readValue(
            Files.readString(systemOperation.resolvePath(Global.homeDirectory, EXCHANGE_FILENAME.toFileName())),
            ExchangeWrapper::class.java,
        ).exportedContent.toPasswordInfoMap()
    } catch (e: IOException) {
        reportFailure(ImportFailure(e))
        emptyMap()
    }

    private fun PasswordInfoMap.toSerializable() = entries.map { nest ->
        EggsPerNest(
            exportedNest = ExportedNest(nest.key.viewNestId().asString(), nest.key.slot.index()),
            exportedEggs = nest.value.map {
                ExportedEgg(
                    eggId = it.first.first.asString(),
                    password = it.first.second.asString(),
                    proteins = it.second.mapIndexed { index, protein ->
                        ExportedProtein(
                            proteinType = protein.first.asString(),
                            proteinStructure = protein.second.asString(),
                            slot = index,
                        )
                    },
                )
            },
        )
    }

    private fun List<EggsPerNest>.toPasswordInfoMap() = associate { entry ->
        createNest(shellOf(entry.exportedNest.nestId), slotAt(entry.exportedNest.slot)) to (
            entry.exportedEggs.map {
                PasswordInfo(
                    first = ShellPair(shellOf(it.eggId), shellOf(it.password)),
                    second = it.proteins.map { protein -> ShellPair(shellOf(protein.proteinType), shellOf(protein.proteinStructure)) },
                )
            }
            )
    }
}

private class ExportedProtein(var proteinType: String = "", var proteinStructure: String = "", var slot: Int = 0)
private class ExportedEgg(var eggId: String = "", var password: String = "", var proteins: List<ExportedProtein> = emptyList())
private class ExportedNest(var nestId: String = "", var slot: Int = 0)
private class EggsPerNest(var exportedNest: ExportedNest = ExportedNest(), var exportedEggs: List<ExportedEgg> = emptyList())
private class ExchangeWrapper(val exportedContent: List<EggsPerNest> = emptyList())
