package de.pflugradts.passbird.adapter.exchange

import com.fasterxml.jackson.databind.json.JsonMapper
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import de.pflugradts.passbird.application.ExchangeAdapterPort
import de.pflugradts.passbird.application.ShellPairMap
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.EXCHANGE_FILENAME
import de.pflugradts.passbird.application.failure.ExportFailure
import de.pflugradts.passbird.application.failure.ImportFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.ShellPair
import java.io.IOException
import java.nio.file.Files

class FilePasswordExchange @Inject constructor(
    @Assisted private val uri: String,
    @Inject private val systemOperation: SystemOperation,
) : ExchangeAdapterPort {
    private val mapper = JsonMapper()

    override fun send(data: ShellPairMap) {
        try {
            Files.writeString(
                systemOperation.resolvePath(uri.toDirectory(), EXCHANGE_FILENAME.toFileName()),
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ExchangeWrapper(data.toSerializable())),
            )
        } catch (e: IOException) {
            reportFailure(ExportFailure(e))
        }
    }

    override fun receive(): ShellPairMap {
        return try {
            mapper.readValue(
                Files.readString(systemOperation.resolvePath(uri.toDirectory(), EXCHANGE_FILENAME.toFileName())),
                ExchangeWrapper::class.java,
            ).value.toShellPairMap()
        } catch (e: IOException) {
            reportFailure(ImportFailure(e))
            emptyMap()
        }
    }
}

private class PlainEgg(var eggId: String = "", var password: String = "")
private typealias PlainEggMap = Map<NestSlot, List<PlainEgg>>
private class ExchangeWrapper(val value: PlainEggMap = emptyMap())
private fun ShellPairMap.toSerializable() = entries.associate { nest ->
    nest.key to nest.value.map { PlainEgg(it.first.asString(), it.second.asString()) }
}
private fun PlainEggMap.toShellPairMap() = entries.associate { nest ->
    nest.key to nest.value.map { ShellPair(shellOf(it.eggId), shellOf(it.password)) }
}
