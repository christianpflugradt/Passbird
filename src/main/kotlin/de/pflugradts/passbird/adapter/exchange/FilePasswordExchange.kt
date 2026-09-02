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
import de.pflugradts.passbird.application.util.withScrambledBytes
import de.pflugradts.passbird.application.yolk.normalizeTotpAlgorithm
import de.pflugradts.passbird.domain.model.egg.requireValidEggId
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.nest.Nest.Companion.createNest
import de.pflugradts.passbird.domain.model.shell.Shell
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
            ZipOutputStream(output, password).use { it.writeEntries(data) }
        }
        Unit
    }.onFailure { reportFailure(ExportFailure(it)) }

    override fun receive(password: CharArray) = tryCatching {
        require(password.isNotEmpty()) { "Import password must not be empty" }
        Files.newInputStream(path()).use { input ->
            ZipInputStream(input, password).use { zip ->
                val entries = mutableMapOf<String, CsvDocument>()
                try {
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        val document = if (entry.fileName == MANIFEST) Csv.manifest(zip) else Csv.read(zip)
                        require(entries.put(entry.fileName, document) == null) { "Duplicate ZIP entry: " + entry.fileName }
                    }
                    entries.toPasswordInfoMap()
                } finally {
                    entries.values.forEach(CsvDocument::scramble)
                }
            }
        }
    }.onFailure { reportFailure(ImportFailure(it)) }

    private fun path() = systemOperation.resolvePath(runContext.homeDirectory, EXCHANGE_FILENAME.toFileName())
}

private fun ZipOutputStream.writeEntries(data: PasswordInfoMap) {
    entryText(MANIFEST, MANIFEST_CONTENT)
    writeNests(data)
    writeEggs(data)
    writeProteins(data)
    writeYolks(data)
}

private fun ZipOutputStream.writeNests(data: PasswordInfoMap) = entry(NESTS) { writer ->
    writer.row(NEST_HEADER.map(::textCell))
    data.keys.forEach { writer.row(listOf(textCell(it.slot.index().toString()), shellCell(it.viewNestId()))) }
}

private fun ZipOutputStream.writeEggs(data: PasswordInfoMap) = entry(EGGS) { writer ->
    writer.row(EGG_HEADER.map(::textCell))
    data.forEach { (nest, eggs) ->
        eggs.forEach { egg ->
            writer.row(listOf(shellCell(egg.first.first), textCell(nest.slot.index().toString()), shellCell(egg.first.second)))
        }
    }
}

private fun ZipOutputStream.writeProteins(data: PasswordInfoMap) = entry(PROTEINS) { writer ->
    writer.row(PROTEIN_HEADER.map(::textCell))
    data.forEach { (nest, eggs) ->
        eggs.forEach { egg ->
            egg.second.forEachIndexed { slot, protein ->
                if (protein.first.isNotEmpty && protein.second.isNotEmpty) {
                    writer.row(
                        listOf(
                            shellCell(egg.first.first),
                            textCell(nest.slot.index().toString()),
                            textCell(slot.toString()),
                            shellCell(protein.first),
                            shellCell(protein.second),
                        ),
                    )
                }
            }
        }
    }
}

private fun ZipOutputStream.writeYolks(data: PasswordInfoMap) = entry(YOLKS) { writer ->
    writer.row(YOLK_HEADER.map(::textCell))
    data.forEach { (nest, eggs) ->
        eggs.forEach { egg ->
            egg.yolk?.let { yolk ->
                val encodedSecret = canonicalBase32(yolk.secret)
                try {
                    writer.row(
                        listOf(
                            shellCell(egg.first.first),
                            textCell(nest.slot.index().toString()),
                            shellCell(encodedSecret),
                            textCell(yolk.algorithm),
                            textCell(yolk.digits.toString()),
                            textCell(yolk.periodSeconds.toString()),
                        ),
                    )
                } finally {
                    encodedSecret.scramble()
                }
            }
        }
    }
}

private fun ZipOutputStream.entry(name: String, writeRows: (CsvWriter) -> Unit) {
    putNextEntry(parameters(name))
    OutputStreamWriter(this, UTF_8).run {
        writeRows(CsvWriter(this))
        flush()
    }
    closeEntry()
}

private fun ZipOutputStream.entryText(name: String, content: String) {
    putNextEntry(parameters(name))
    write(content.toByteArray(UTF_8))
    closeEntry()
}

private fun parameters(name: String) = ZipParameters().apply {
    fileNameInZip = name
    isEncryptFiles = true
    encryptionMethod = EncryptionMethod.AES
    aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
}

private fun Map<String, CsvDocument>.toPasswordInfoMap(): PasswordInfoMap {
    require(get(MANIFEST)?.matches(MANIFEST_CONTENT) == true) { "Unsupported export manifest" }
    val nestRows = records(NESTS, NEST_HEADER)
    val nests = nestRows.associate { row ->
        val slot = row[0].metadataInt().toSlot()
        slot to createNest(row[1].copy().also { require(it.isNotEmpty) }, slot)
    }
    require(nests.size == nestRows.size) { "Duplicate nest slot in import file" }
    val eggs = records(EGGS, EGG_HEADER).map { row ->
        val slot = row[1].metadataInt().toSlot()
        val eggId = row[0].copy()
        requireValidEggId(eggId)
        require(slot in nests) { "Egg references missing nest" }
        EggRow(eggId, slot, row[2].copy())
    }
    require(eggs.map { it.eggId to it.slot }.toSet().size == eggs.size) { "Duplicate eggId in import file" }
    val eggKeys = eggs.map { it.slot to it.eggId }.toSet()
    val proteins = records(PROTEINS, PROTEIN_HEADER).groupBy { row -> row[1].metadataInt().toSlot() to row[0] }
    val yolkRows = records(YOLKS, YOLK_HEADER)
    val yolks = yolkRows.associateBy { row -> row[1].metadataInt().toSlot() to row[0] }
    require(yolks.size == yolkRows.size) { "Duplicate yolk record" }
    require(proteins.keys.all(eggKeys::contains) && yolks.keys.all(eggKeys::contains)) { "Record references missing egg" }
    return nests.values.associateWith { nest ->
        eggs.filter { it.slot == nest.slot }.map { egg ->
            PasswordInfo(
                ShellPair(egg.eggId, egg.password),
                proteins[nest.slot to egg.eggId].toProteins(),
                yolks[nest.slot to egg.eggId]?.toYolk(),
            )
        }
    }
}

private fun Map<String, CsvDocument>.records(name: String, header: List<String>): List<List<Shell>> {
    val rows = requireNotNull(get(name)) { "Missing " + name }.rows
    require(rows.firstOrNull()?.matchesHeader(header) == true) { "Invalid " + name + " header" }
    return rows.drop(1).onEach { require(it.size == header.size) { "Invalid " + name + " row" } }
}

private fun List<List<Shell>>?.toProteins(): List<ShellPair> {
    val records = this ?: emptyList()
    val presentRecords = records.map { row ->
        val slot = row[2].metadataInt().also { require(it in Slot.entries.indices) { "Invalid protein slot" } }
        require(row[3].isEmpty == row[4].isEmpty) { "Partial protein record" }
        slot to ShellPair(row[3].copy(), row[4].copy())
    }.filter { (_, protein) -> protein.first.isNotEmpty }
    val bySlot = presentRecords.toMap()
    require(bySlot.size == presentRecords.size) { "Duplicate protein slot" }
    return Slot.entries.indices.map { bySlot[it] ?: ShellPair(emptyShell(), emptyShell()) }
}

private fun List<Shell>.toYolk() = PasswordYolkInfo(
    decodeCanonicalBase32(this[2]),
    normalizeTotpAlgorithm(this[3].metadataString()),
    this[4].metadataInt(),
    this[5].metadataInt(),
)

private fun Int.toSlot(): Slot = Slot.entries.getOrNull(this) ?: error("Invalid nest slot")
private data class EggRow(val eggId: Shell, val slot: Slot, val password: Shell)

private class CsvWriter(private val output: OutputStreamWriter) {
    fun row(cells: List<CsvCell>) {
        cells.forEachIndexed { index, cell ->
            if (index > 0) output.write(','.code)
            output.write('\"'.code)
            cell.writeEscapedTo(output)
            output.write('\"'.code)
        }
        output.write('\n'.code)
    }
}

private sealed interface CsvCell {
    fun writeEscapedTo(output: OutputStreamWriter)
}
private class ShellCell(private val value: Shell) : CsvCell {
    override fun writeEscapedTo(output: OutputStreamWriter) = withScrambledBytes(value.toByteArray()) { bytes ->
        val chars = CharArray(bytes.size) { bytes[it].toInt().toChar() }
        try {
            chars.forEach { char ->
                if (char == '\"') output.write('\"'.code)
                output.write(char.code)
            }
        } finally {
            chars.fill('\u0000')
        }
    }
}
private class TextCell(private val value: String) : CsvCell {
    override fun writeEscapedTo(output: OutputStreamWriter) {
        value.forEach { char ->
            if (char == '\"') output.write('\"'.code)
            output.write(char.code)
        }
    }
}
private fun shellCell(value: Shell) = ShellCell(value)
private fun textCell(value: String) = TextCell(value)

private class CsvDocument(val rows: List<List<Shell>>) {
    fun scramble() = rows.flatten().forEach(Shell::scramble)
    fun matches(content: String) = rows.singleOrNull()?.singleOrNull()?.matchesAscii(content) == true
}

private object Csv {
    fun manifest(input: InputStream): CsvDocument {
        val content = ByteBuffer()
        input.reader(UTF_8).let { reader ->
            val chars = CharArray(1024)
            try {
                while (true) {
                    val count = reader.read(chars)
                    if (count < 0) break
                    for (index in 0 until count) content.append(chars[index])
                }
            } finally {
                chars.fill('\u0000')
            }
        }
        return CsvDocument(listOf(listOf(content.toShell())))
    }

    fun read(input: InputStream): CsvDocument {
        val parser = Parser()
        input.reader(UTF_8).let { reader ->
            val chars = CharArray(1024)
            try {
                while (true) {
                    val count = reader.read(chars)
                    if (count < 0) break
                    for (index in 0 until count) parser.consume(chars[index])
                }
            } finally {
                chars.fill('\u0000')
            }
        }
        return parser.finish()
    }

    private class Parser {
        private val rows = mutableListOf<List<Shell>>()
        private val fields = mutableListOf<Shell>()
        private var field = ByteBuffer()
        private var quoted = false
        private var quotePending = false

        fun consume(char: Char) {
            if (quotePending) {
                if (char == '\"') {
                    field.append(char)
                    quotePending = false
                    return
                }
                quoted = false
                quotePending = false
                consume(char)
                return
            }
            when (char) {
                '\"' -> if (quoted) quotePending = true else quoted = true
                ',' -> if (quoted) field.append(char) else addField()
                '\n' -> if (quoted) field.append(char) else addRow()
                '\r' -> if (quoted) field.append(char)
                else -> field.append(char)
            }
        }

        fun finish(): CsvDocument {
            if (quotePending) quoted = false
            require(!quoted) { "Unterminated CSV field" }
            if (fields.isNotEmpty() || field.isNotEmpty) addRow()
            return CsvDocument(rows)
        }

        private fun addField() {
            fields += field.toShell()
            field = ByteBuffer()
        }
        private fun addRow() {
            addField()
            rows += fields.toList()
            fields.clear()
        }
    }
}

private class ByteBuffer(initialCapacity: Int = 32) {
    private var bytes = ByteArray(initialCapacity)
    private var size = 0
    val isNotEmpty get() = size > 0
    fun append(char: Char) {
        require(char.code <= 0xff) { "Unsupported CSV character" }
        if (size == bytes.size) grow()
        bytes[size++] = char.code.toByte()
    }
    fun toShell() = try {
        withScrambledBytes(bytes.copyOf(size)) { shellOf(it) }
    } finally {
        bytes.fill(0)
    }
    private fun grow() {
        val replacement = ByteArray(bytes.size * 2)
        bytes.copyInto(replacement)
        bytes.fill(0)
        bytes = replacement
    }
}

private fun List<Shell>.matchesHeader(header: List<String>) = size == header.size && indices.all { this[it].matchesAscii(header[it]) }
private fun Shell.matchesAscii(text: String) = size == text.length && text.indices.all { getByte(it) == text[it].code.toByte() }
private fun Shell.metadataString() = CharArray(size) { getChar(it) }.concatToString()
private fun Shell.metadataInt() = metadataString().toInt()

private fun canonicalBase32(secret: Shell): Shell = withScrambledBytes(ByteArray((secret.size * 8 + 4) / 5)) { encoded ->
    var buffer = 0
    var bits = 0
    var size = 0
    for (byte in secret) {
        buffer = (buffer shl 8) or (byte.toInt() and 0xff)
        bits += 8
        while (bits >= 5) {
            bits -= 5
            encoded[size++] = BASE32[(buffer shr bits) and 31]
        }
    }
    if (bits > 0) encoded[size++] = BASE32[(buffer shl (5 - bits)) and 31]
    withScrambledBytes(encoded.copyOf(size)) { shellOf(it) }
}

private fun decodeCanonicalBase32(encoded: Shell): Shell {
    require(encoded.isNotEmpty) { "Empty Base32 secret" }
    require(encoded.size % 8 !in setOf(1, 3, 6)) { "Invalid Base32 secret" }
    return withScrambledBytes(ByteArray(encoded.size * 5 / 8)) { decoded ->
        var buffer = 0
        var bits = 0
        var size = 0
        for (byte in encoded) {
            val value = byte.base32Value()
            require(value >= 0) { "Invalid Base32 secret" }
            buffer = (buffer shl 5) or value
            bits += 5
            while (bits >= 8) {
                bits -= 8
                decoded[size++] = (buffer shr bits).toByte()
            }
        }
        require(bits == 0 || (buffer and ((1 shl bits) - 1)) == 0) { "Invalid Base32 secret" }
        withScrambledBytes(decoded.copyOf(size)) { shellOf(it) }
    }
}

private fun Byte.base32Value(): Int = when (val code = toInt() and 0xff) {
    in 'A'.code..'Z'.code -> code - 'A'.code
    in '2'.code..'7'.code -> code - '2'.code + 26
    else -> -1
}

private val BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".encodeToByteArray()
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
