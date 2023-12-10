package de.pflugradts.passbird.adapter.exchange

import com.fasterxml.jackson.databind.json.JsonMapper
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import de.pflugradts.passbird.application.ExchangeAdapterPort
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.EXCHANGE_FILENAME
import de.pflugradts.passbird.application.failure.ExportFailure
import de.pflugradts.passbird.application.failure.ImportFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.BytePair
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import java.io.IOException
import java.nio.file.Files

class FilePasswordExchange @Inject constructor(
    @Assisted private val uri: String,
    @Inject private val systemOperation: SystemOperation,
) : ExchangeAdapterPort {
    private val mapper = JsonMapper()

    override fun send(data: Map<Slot, List<BytePair>>) {
        try {
            Files.writeString(
                systemOperation.resolvePath(uri, EXCHANGE_FILENAME),
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ExchangeWrapper(data.toSerializable())),
            )
        } catch (e: IOException) {
            reportFailure(ExportFailure(e))
        }
    }

    override fun receive(): Map<Slot, List<BytePair>> {
        return try {
            mapper.readValue(
                Files.readString(systemOperation.resolvePath(uri, EXCHANGE_FILENAME)),
                ExchangeWrapper::class.java,
            ).value.toBytePairMap()
        } catch (e: IOException) {
            reportFailure(ImportFailure(e))
            emptyMap()
        }
    }
}

private class ExchangeWrapper(val value: Map<Slot, List<PlainPasswordEntry>> = emptyMap())
private class PlainPasswordEntry(var alias: String = "", var password: String = "")
private fun Map<Slot, List<BytePair>>.toSerializable() = entries.associate { nest ->
    nest.key to nest.value.map { PlainPasswordEntry(it.value.first.asString(), it.value.second.asString()) }
}
private fun Map<Slot, List<PlainPasswordEntry>>.toBytePairMap() = entries.associate { nest ->
    nest.key to nest.value.map { BytePair(Pair(bytesOf(it.alias), bytesOf(it.password))) }
}
