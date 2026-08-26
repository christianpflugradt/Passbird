package de.pflugradts.passbird.application.yolk

import de.pflugradts.passbird.application.util.withScrambledBytes
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import java.net.URI
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class TotpCode(
    val value: String,
    val remainingValiditySeconds: Int,
)

data class ParsedYolk(
    val secret: Shell,
    val algorithm: String,
    val digits: Int,
    val periodSeconds: Int,
)

data class ConfiguredYolkDefaults(
    val algorithm: String,
    val digits: Int,
    val periodSeconds: Int,
)

class TotpGenerator(
    private val clock: Clock,
) {
    fun generate(secret: ByteArray, algorithm: String, digits: Int, periodSeconds: Int): TotpCode {
        require(digits == 6 || digits == 8) { "Unsupported digits" }
        require(periodSeconds > 0) { "Unsupported period" }
        val supportedAlgorithm = parseTotpAlgorithm(algorithm)
        val timestamp = Instant.now(clock).epochSecond
        val counter = timestamp / periodSeconds
        val elapsed = (timestamp % periodSeconds).toInt()
        val remaining = if (elapsed == 0) periodSeconds else periodSeconds - elapsed
        val mac = Mac.getInstance(supportedAlgorithm.hmacName)
        mac.init(SecretKeySpec(secret, supportedAlgorithm.hmacName))
        val hash = mac.doFinal(ByteBuffer.allocate(java.lang.Long.BYTES).putLong(counter).array())
        val offset = hash.last().toInt() and 0x0f
        val binary = ((hash[offset].toInt() and 0x7f) shl 24) or
            ((hash[offset + 1].toInt() and 0xff) shl 16) or
            ((hash[offset + 2].toInt() and 0xff) shl 8) or
            (hash[offset + 3].toInt() and 0xff)
        val modulus = if (digits == 6) 1_000_000 else 100_000_000
        return TotpCode(
            value = (binary % modulus).toString().padStart(digits, '0'),
            remainingValiditySeconds = remaining,
        )
    }
}

class YolkInputParser {
    fun parse(shell: Shell, configuredDefaults: ConfiguredYolkDefaults = ConfiguredYolkDefaults(defaultTotpAlgorithm(), 6, 30)) =
        if (shell.startsWithAsciiIgnoreCase("otpauth://")) {
            parseOtpauthUri(shell.asString())
        } else {
            ParsedYolk(
                secret = decodeBase32Secret(shell),
                algorithm = normalizeTotpAlgorithm(configuredDefaults.algorithm),
                digits = configuredDefaults.digits.also { require(it == 6 || it == 8) { "Unsupported digits" } },
                periodSeconds = configuredDefaults.periodSeconds.also { require(it > 0) { "Unsupported period" } },
            )
        }

    private fun parseOtpauthUri(input: String): ParsedYolk {
        val uri = runCatching { URI(input) }.getOrElse { throw IllegalArgumentException("Malformed otpauth URI") }
        require(uri.scheme.equals("otpauth", ignoreCase = true)) { "Unsupported URI scheme" }
        require(uri.host.equals("totp", ignoreCase = true)) { "Unsupported URI type" }
        val params = queryParams(uri.rawQuery ?: "")
        val secret = params["secret"]?.takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("Missing secret")
        val algorithm = params["algorithm"]?.let(::normalizeTotpAlgorithm) ?: defaultTotpAlgorithm()
        val digits = params["digits"]?.toIntOrNull() ?: 6
        val periodSeconds = params["period"]?.toIntOrNull() ?: 30
        require(digits == 6 || digits == 8) { "Unsupported digits" }
        require(periodSeconds > 0) { "Unsupported period" }
        return ParsedYolk(
            secret = decodeBase32Secret(shellOf(secret)),
            algorithm = algorithm,
            digits = digits,
            periodSeconds = periodSeconds,
        )
    }

    private fun queryParams(rawQuery: String) = rawQuery
        .split("&")
        .filter { it.isNotBlank() }
        .associate {
            val parts = it.split("=", limit = 2)
            val key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8)
            val value = URLDecoder.decode(parts.getOrElse(1) { "" }, StandardCharsets.UTF_8)
            key to value
        }

    private fun decodeBase32Secret(rawInput: Shell): Shell = withScrambledBytes(ByteArray(rawInput.size)) { normalized ->
        val normalizedSize = copyNormalizedInput(rawInput, normalized)
        require(normalizedSize > 0) { "Empty secret" }
        val unpaddedSize = normalized.trimmedPaddingSize(normalizedSize)
        require(unpaddedSize > 0) { "Empty secret" }
        withScrambledBytes(ByteArray(unpaddedSize * 5 / 8)) { decoded ->
            var decodedIndex = 0
            var buffer = 0
            var bitsLeft = 0
            for (index in 0 until unpaddedSize) {
                val value = normalized[index].base32Value()
                require(value >= 0) { "Invalid Base32 secret" }
                buffer = (buffer shl 5) or value
                bitsLeft += 5
                while (bitsLeft >= 8) {
                    bitsLeft -= 8
                    decoded[decodedIndex++] = ((buffer shr bitsLeft) and 0xff).toByte()
                }
            }
            shellOf(decoded)
        }
    }

    private fun copyNormalizedInput(rawInput: Shell, normalized: ByteArray): Int {
        var normalizedSize = 0
        for (byte in rawInput) {
            if (!byte.isIgnoredBase32Separator()) {
                normalized[normalizedSize++] = byte.uppercaseAscii()
            }
        }
        return normalizedSize
    }

    private fun ByteArray.trimmedPaddingSize(size: Int): Int {
        var trimmedSize = size
        while (trimmedSize > 0 && this[trimmedSize - 1] == '='.code.toByte()) {
            trimmedSize--
        }
        return trimmedSize
    }

    private fun Byte.base32Value(): Int {
        val code = toInt() and 0xff
        return when (code) {
            in 'A'.code..'Z'.code -> code - 'A'.code
            in '2'.code..'7'.code -> code - '2'.code + 26
            else -> -1
        }
    }

    private fun Byte.uppercaseAscii(): Byte {
        val code = toInt() and 0xff
        return if (code in 'a'.code..'z'.code) {
            (code - ('a'.code - 'A'.code)).toByte()
        } else {
            code.toByte()
        }
    }

    private fun Byte.isIgnoredBase32Separator() = isWhitespace() || this == '-'.code.toByte() || this == '_'.code.toByte()

    private fun Byte.isWhitespace() = when (toInt() and 0xff) {
        ' '.code, '\n'.code, '\r'.code, '\t'.code -> true
        else -> false
    }

    private fun Shell.startsWithAsciiIgnoreCase(prefix: String): Boolean {
        if (size < prefix.length) return false
        for (index in prefix.indices) {
            if (getByte(index).uppercaseAscii() != prefix[index].code.toByte().uppercaseAscii()) return false
        }
        return true
    }
}

fun defaultTotpAlgorithm() = SupportedTotpAlgorithm.SHA1.storedName

fun normalizeTotpAlgorithm(value: String) = parseTotpAlgorithm(value).storedName

fun totpAlgorithmOrdinal(value: String) = parseTotpAlgorithm(value).ordinal

fun totpAlgorithmAtOrdinal(ordinal: Int) = SupportedTotpAlgorithm.entries.getOrNull(ordinal)?.storedName
    ?: throw IllegalStateException("Unsupported stored yolk algorithm.")

private fun parseTotpAlgorithm(value: String) = SupportedTotpAlgorithm.entries.find { it.storedName.equals(value, ignoreCase = true) }
    ?: throw IllegalArgumentException("Unsupported algorithm")

private enum class SupportedTotpAlgorithm(
    val storedName: String,
    val hmacName: String,
) {
    SHA1("SHA1", "HmacSHA1"),
    SHA256("SHA256", "HmacSHA256"),
    SHA512("SHA512", "HmacSHA512"),
}
