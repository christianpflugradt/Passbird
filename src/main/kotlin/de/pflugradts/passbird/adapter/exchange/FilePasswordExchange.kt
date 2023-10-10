package de.pflugradts.passbird.adapter.exchange

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import de.pflugradts.passbird.application.BytePair
import de.pflugradts.passbird.application.ExchangeAdapterPort
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.EXCHANGE_FILENAME
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
import lombok.AllArgsConstructor
import java.io.IOException
import java.nio.file.Files
import java.util.stream.Stream

@AllArgsConstructor
class FilePasswordExchange @Inject constructor(
    @param:Assisted private val uri: String,
    @Inject private val systemOperation: SystemOperation,
) : ExchangeAdapterPort {
    private val mapper = YAMLMapper()

    override fun send(data: Stream<BytePair>) {
        try {
            Files.writeString(
                systemOperation.resolvePath(uri, EXCHANGE_FILENAME),
                mapper.writeValueAsString(PasswordEntriesRepresentation(data.map { it.asPasswordEntryRepresentation() }.toList())),
            )
        } catch (e: IOException) {
            // FIXME error handling
        }
    }

    override fun receive(): Stream<BytePair> {
        return try {
            mapper.readValue(
                Files.readString(systemOperation.resolvePath(uri, EXCHANGE_FILENAME)),
                PasswordEntriesRepresentation::class.java,
            ).passwordEntryRepresentations?.stream()?.map { it.asBytesPair() } ?: Stream.empty()
        } catch (e: IOException) {
            // FIXME error handling
            Stream.empty()
        }
    }
}

data class PasswordEntryRepresentation(var key: String? = null, var password: String? = null) {
    fun asBytesPair() = if (key.isNullOrBlank() || password.isNullOrBlank()) {
        BytePair(Pair(emptyBytes(), emptyBytes()))
    } else {
        BytePair(Pair(bytesOf(key!!), bytesOf(password!!)))
    }
}

data class PasswordEntriesRepresentation(
    @JsonProperty("passwordEntry") val passwordEntryRepresentations: List<PasswordEntryRepresentation>? = null,
)

fun BytePair.asPasswordEntryRepresentation() = PasswordEntryRepresentation(value.first.asString(), value.second.asString())
