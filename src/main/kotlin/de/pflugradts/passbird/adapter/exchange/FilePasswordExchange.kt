package de.pflugradts.passbird.adapter.exchange

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
import de.pflugradts.passbird.application.yolk.normalizeTotpAlgorithm
import de.pflugradts.passbird.domain.model.egg.requireValidEggId
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.createNest
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.model.slot.Slot
import net.lingala.zip4j.io.inputstream.ZipInputStream
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.InputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files

class FilePasswordExchange(
    private val systemOperation: SystemOperation,
    private val runContext: RunContext,
) : ExchangeAdapterPort {
    override fun send(data: PasswordInfoMap, password: CharArray) = tryCatching {
        require(password.isNotEmpty()) { "Export password must not be empty" }
        systemOperation.writeToSensitiveFile(path()) { output ->
            ZipOutputStream(output, password).use { zip ->
                zip.entryText(MANIFEST, MANIFEST_CONTENT)
                zip.entry(NESTS, listOf(NEST_HEADER) + data.keys.map { listOf(it.slot.index().toString(), it.viewNestId().asString()) })
                zip.entry(
                    EGGS,
                    listOf(EGG_HEADER) +
                        data.flatMap { (nest, eggs) ->
                            eggs.map { listOf(it.first.first.asString(), nest.slot.index().toString(), it.first.second.asString()) }
                        },
                )
                zip.entry(
                    PROTEINS,
                    listOf(PROTEIN_HEADER) +
                        data.flatMap { (nest, eggs) ->
                            eggs.flatMap { egg ->
                                egg.second.mapIndexed { slot, protein ->
                                    listOf(
                                        egg.first.first.asString(),
                                        nest.slot.index().toString(),
                                        slot.toString(),
                                        protein.first.asString(),
                                        protein.second.asString(),
                                    )
                                }
                            }
                        },
                )
                zip.entry(
                    YOLKS,
                    listOf(YOLK_HEADER) +
                        data.flatMap { (nest, eggs) ->
                            eggs.mapNotNull { egg ->
                                egg.yolk?.let {
                                    listOf(
                                        egg.first.first.asString(),
                                        nest.slot.index().toString(),
                                        it.secret.asString(),
                                        it.algorithm,
                                        it.digits.toString(),
                                        it.periodSeconds.toString(),
                                    )
                                }
                            }
                        },
                )
            }
        }
        Unit
    }.onFailure { reportFailure(ExportFailure(it)) }

    override fun receive(password: CharArray) = tryCatching {
        require(password.isNotEmpty()) { "Import password must not be empty" }
        Files.newInputStream(path()).use { input ->
            ZipInputStream(input, password).use { zip ->
                val entries = mutableMapOf<String, List<List<String>>>()
                while (true) {
                    val entry = zip.nextEntry ?: break
                    require(
                        entries.put(
                            entry.fileName,
                            if (entry.fileName == MANIFEST) listOf(listOf(zip.readBytes().toString(UTF_8))) else Csv.read(zip),
                        ) == null,
                    ) { "Duplicate ZIP entry: " + entry.fileName }
                }
                entries.toPasswordInfoMap()
            }
        }
    }.onFailure { reportFailure(ImportFailure(it)) }

    private fun path() = systemOperation.resolvePath(runContext.homeDirectory, EXCHANGE_FILENAME.toFileName())
}

private fun ZipOutputStream.entry(name: String, rows: List<List<String>>) {
    putNextEntry(
        ZipParameters().apply {
            fileNameInZip = name
            isEncryptFiles = true
            encryptionMethod = EncryptionMethod.AES
            aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
        },
    )
    OutputStreamWriter(this, UTF_8).run {
        rows.forEach { write(it.joinToString(",") { value -> "\"" + value.replace("\"", "\"\"") + "\"" } + "\n") }
        flush()
    }
    closeEntry()
}

private fun ZipOutputStream.entryText(name: String, content: String) {
    putNextEntry(
        ZipParameters().apply {
            fileNameInZip = name
            isEncryptFiles = true
            encryptionMethod = EncryptionMethod.AES
            aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
        },
    )
    write(content.toByteArray(UTF_8))
    closeEntry()
}

private fun Map<String, List<List<String>>>.toPasswordInfoMap(): PasswordInfoMap {
    require(get(MANIFEST)?.singleOrNull()?.singleOrNull() == MANIFEST_CONTENT) { "Unsupported export manifest" }
    val nestRows = records(NESTS, NEST_HEADER)
    val nests = nestRows.associate { row ->
        val slot = row[0].toInt().toSlot()
        slot to createNest(shellOf(row[1].also { require(it.isNotBlank()) }), slot)
    }
    require(nests.size == nestRows.size) { "Duplicate nest slot in import file" }
    val eggs = records(EGGS, EGG_HEADER).map { row ->
        val slot = row[1].toInt().toSlot()
        requireValidEggId(shellOf(row[0]))
        require(slot in nests) { "Egg references missing nest" }
        EggRow(row[0], slot, row[2])
    }
    require(eggs.map { it.eggId to it.slot }.toSet().size == eggs.size) { "Duplicate eggId in import file" }
    val eggKeys = eggs.map { it.slot to it.eggId }.toSet()
    val proteins = records(PROTEINS, PROTEIN_HEADER).groupBy { row ->
        row[1].toInt().toSlot() to row[0]
    }
    val yolkRows = records(YOLKS, YOLK_HEADER)
    val yolks = yolkRows.associateBy { row -> row[1].toInt().toSlot() to row[0] }
    require(yolks.size == yolkRows.size) { "Duplicate yolk record" }
    require(proteins.keys.all(eggKeys::contains) && yolks.keys.all(eggKeys::contains)) { "Record references missing egg" }
    return nests.values.associateWith { nest ->
        eggs.filter { it.slot == nest.slot }.map { egg ->
            PasswordInfo(
                ShellPair(shellOf(egg.eggId), shellOf(egg.password)),
                proteins[nest.slot to egg.eggId].toProteins(),
                yolks[nest.slot to egg.eggId]?.toYolk(),
            )
        }
    }
}

private fun Map<String, List<List<String>>>.records(name: String, header: List<String>): List<List<String>> {
    val rows = requireNotNull(get(name)) { "Missing " + name }
    require(rows.firstOrNull() == header) { "Invalid " + name + " header" }
    return rows.drop(1).onEach { require(it.size == header.size) { "Invalid " + name + " row" } }
}

private fun List<List<String>>?.toProteins(): List<ShellPair> {
    val records = this ?: emptyList()
    val bySlot = records.associate { row ->
        val slot = row[2].toInt().also { require(it in Slot.entries.indices) { "Invalid protein slot" } }
        require(row[3].isEmpty() == row[4].isEmpty()) { "Partial protein record" }
        slot to ShellPair(shellOf(row[3]), shellOf(row[4]))
    }
    require(bySlot.size == records.size) { "Duplicate protein slot" }
    return Slot.entries.indices.map { bySlot[it] ?: ShellPair(emptyShell(), emptyShell()) }
}

private fun List<String>.toYolk() = PasswordYolkInfo(
    shellOf(
        this[2].also {
            require(it.isNotBlank())
        },
    ),
    normalizeTotpAlgorithm(this[3]),
    this[4].toInt(),
    this[5].toInt(),
)
private fun Int.toSlot(): Slot = Slot.entries.getOrNull(this) ?: error("Invalid nest slot")
private data class EggRow(val eggId: String, val slot: Slot, val password: String)

private object Csv {
    fun read(input: InputStream) = Parser(input.readBytes().toString(UTF_8)).parse()

    private class Parser(private val content: String) {
        private val rows = mutableListOf<List<String>>()
        private val fields = mutableListOf<String>()
        val field = StringBuilder()
        private var quoted = false

        fun parse(): List<List<String>> {
            var index = 0
            while (index < content.length) {
                index += consume(index)
            }
            require(!quoted) { "Unterminated CSV field" }
            if (fields.isNotEmpty() || field.isNotEmpty()) {
                addRow()
            }
            return rows
        }

        private fun consume(index: Int): Int = when (val current = content[index]) {
            '"' -> quote(index)
            ',' -> delimiter(current)
            '\n' -> newline(current)
            '\r' -> carriageReturn(current)
            else -> append(current)
        }

        private fun quote(index: Int) = if (quoted && index + 1 < content.length && content[index + 1] == '"') {
            field.append('"')
            2
        } else {
            quoted = !quoted
            1
        }

        private fun delimiter(current: Char): Int {
            if (quoted) {
                field.append(current)
            } else {
                fields += field.toString()
                field.clear()
            }
            return 1
        }

        private fun newline(current: Char): Int {
            if (quoted) {
                field.append(current)
            } else {
                addRow()
            }
            return 1
        }

        private fun carriageReturn(current: Char): Int {
            if (quoted) {
                field.append(current)
            }
            return 1
        }

        private fun append(current: Char): Int {
            field.append(current)
            return 1
        }

        private fun addRow() {
            fields += field.toString()
            rows += fields.toList()
            fields.clear()
            field.clear()
        }
    }
}

private const val MANIFEST = "manifest.txt"
private const val NESTS = "nests.csv"
private const val EGGS = "eggs.csv"
private const val PROTEINS = "proteins.csv"
private const val YOLKS = "yolks.csv"
private const val MANIFEST_CONTENT =
    "format=passbird-export\nversion=1\nencryption=zip-aes-256\n" +
        "files=nests.csv,eggs.csv,proteins.csv,yolks.csv\n"
private val NEST_HEADER = listOf("nest_slot", "nest_name")
private val EGG_HEADER = listOf("egg_id", "nest_slot", "password")
private val PROTEIN_HEADER = listOf("egg_id", "nest_slot", "slot", "type", "structure")
private val YOLK_HEADER = listOf("egg_id", "nest_slot", "secret", "algorithm", "digits", "period")
