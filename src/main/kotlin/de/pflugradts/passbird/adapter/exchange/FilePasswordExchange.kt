package de.pflugradts.passbird.adapter.exchange
import com.fasterxml.jackson.databind.json.JsonMapper
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.ExchangeAdapterPort
import de.pflugradts.passbird.application.PasswordInfo
import de.pflugradts.passbird.application.PasswordInfoMap
import de.pflugradts.passbird.application.PasswordYolkInfo
import de.pflugradts.passbird.application.RunContext
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.EXCHANGE_FILENAME
import de.pflugradts.passbird.application.failure.ExportFailure
import de.pflugradts.passbird.application.failure.ImportFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.yolk.defaultTotpAlgorithm
import de.pflugradts.passbird.application.yolk.normalizeTotpAlgorithm
import de.pflugradts.passbird.domain.model.egg.requireValidEggId
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.createNest
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.model.slot.Slot
import java.nio.file.Files
class FilePasswordExchange constructor(
    private val systemOperation: SystemOperation,
    private val runContext: RunContext,
) : ExchangeAdapterPort {
    private val mapper = JsonMapper()
    override fun send(data: PasswordInfoMap) = tryCatching {
        systemOperation.writeToSensitiveFile(
            systemOperation.resolvePath(runContext.homeDirectory, EXCHANGE_FILENAME.toFileName()),
        ) { outputStream ->
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputStream, ExchangeWrapper(data.toSerializable()))
        }
        Unit
    }.onFailure { reportFailure(ExportFailure(it)) }
    override fun receive() = tryCatching {
        mapper.readValue(
            Files.readString(systemOperation.resolvePath(runContext.homeDirectory, EXCHANGE_FILENAME.toFileName())),
            ExchangeWrapper::class.java,
        ).exportedContent.toPasswordInfoMap()
    }.onFailure { reportFailure(ImportFailure(it)) }
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
                    yolk = it.yolk?.let { yolk ->
                        ExportedYolk(
                            secret = yolk.secret.asString(),
                            algorithm = yolk.algorithm,
                            digits = yolk.digits,
                            period = yolk.periodSeconds,
                        )
                    },
                )
            },
        )
    }
    private fun List<EggsPerNest>.toPasswordInfoMap() = associate { entry ->
        entry.exportedNest.toValidatedNest() to
            entry.exportedEggs.toValidatedPasswordInfos()
    }.also { passwordInfoMap ->
        require(passwordInfoMap.size == size) { "Duplicate nest slot in import file" }
    }
    private fun List<ExportedEgg>.toValidatedPasswordInfos(): List<PasswordInfo> {
        val eggIds = mutableSetOf<String>()
        forEach {
            it.validateEggId(eggIds)
        }
        return map {
            PasswordInfo(
                first = ShellPair(shellOf(it.eggId), shellOf(it.password)),
                second = it.proteins.toShellPairsBySlot(),
                yolk = it.yolk?.toValidatedPasswordYolkInfo(),
            )
        }
    }
    private fun ExportedEgg.validateEggId(eggIds: MutableSet<String>) {
        val eggIdShell = shellOf(eggId)
        requireValidEggId(eggIdShell)
        eggIdShell.scramble()
        require(eggIds.add(eggId)) { "Duplicate eggId in import file" }
    }
    private fun ExportedNest.toValidatedSlot(): Slot {
        val slot = requireNotNull(slot) { "Missing nest slot in import file" }
        require(slot in Slot.entries.indices) { "Invalid nest slot $slot" }
        return Slot.entries[slot]
    }
    private fun ExportedNest.toValidatedNest(): Nest {
        require(nestId.isNotBlank()) { "Missing nestId in import file" }
        return createNest(shellOf(nestId), toValidatedSlot())
    }
    private fun List<ExportedProtein>.toShellPairsBySlot(): List<ShellPair> {
        val proteinsBySlot = mutableMapOf<Int, ShellPair>()
        forEach { protein ->
            val slot = requireNotNull(protein.slot) { "Missing protein slot in import file" }
            require(slot in Slot.entries.indices) { "Invalid protein slot $slot" }
            require(protein.proteinType.isEmpty() == protein.proteinStructure.isEmpty()) {
                "Partial protein record in import file"
            }
            require(
                proteinsBySlot.putIfAbsent(
                    slot,
                    ShellPair(shellOf(protein.proteinType), shellOf(protein.proteinStructure)),
                ) == null,
            ) { "Duplicate protein slot $slot" }
        }
        return Slot.entries.indices.map { slot ->
            proteinsBySlot[slot] ?: ShellPair(emptyShell(), emptyShell())
        }
    }
    private fun ExportedYolk.toValidatedPasswordYolkInfo(): PasswordYolkInfo {
        require(secret.isNotBlank()) { "Missing yolk secret in import file" }
        val algorithmName = algorithm?.takeIf { it.isNotBlank() } ?: defaultTotpAlgorithm()
        val actualDigits = digits ?: 6
        val actualPeriod = period ?: 30
        return PasswordYolkInfo(
            secret = shellOf(secret),
            algorithm = normalizeTotpAlgorithm(algorithmName),
            digits = actualDigits.also { require(it == 6 || it == 8) { "Unsupported yolk digits" } },
            periodSeconds = actualPeriod.also { require(it > 0) { "Unsupported yolk period" } },
        )
    }
}
private class ExportedProtein(var proteinType: String = "", var proteinStructure: String = "", var slot: Int? = null)
private class ExportedYolk(
    var secret: String = "",
    var algorithm: String? = null,
    var digits: Int? = null,
    var period: Int? = null,
)
private class ExportedEgg(
    var eggId: String = "",
    var password: String = "",
    var proteins: List<ExportedProtein> = emptyList(),
    var yolk: ExportedYolk? = null,
)
private class ExportedNest(var nestId: String = "", var slot: Int? = null)
private class EggsPerNest(var exportedNest: ExportedNest = ExportedNest(), var exportedEggs: List<ExportedEgg> = emptyList())
private class ExchangeWrapper(val exportedContent: List<EggsPerNest> = emptyList())
