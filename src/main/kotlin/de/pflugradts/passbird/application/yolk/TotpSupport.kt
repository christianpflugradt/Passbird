package de.pflugradts.passbird.application.yolk

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
    fun parse(
        shell: Shell,
        configuredDefaults: ConfiguredYolkDefaults = ConfiguredYolkDefaults(defaultTotpAlgorithm(), 6, 30),
    ): ParsedYolk {
        val input = shell.asString()
        return if (input.startsWith("otpauth://", ignoreCase = true)) {
            parseOtpauthUri(input)
        } else {
            ParsedYolk(
                secret = decodeBase32Secret(input),
                algorithm = normalizeTotpAlgorithm(configuredDefaults.algorithm),
                digits = configuredDefaults.digits.also { require(it == 6 || it == 8) { "Unsupported digits" } },
                periodSeconds = configuredDefaults.periodSeconds.also { require(it > 0) { "Unsupported period" } },
            )
        }
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
            secret = decodeBase32Secret(secret),
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

    private fun decodeBase32Secret(rawInput: String): Shell {
        val normalized = rawInput
            .filterNot { it.isWhitespace() || it == '-' || it == '_' }
            .uppercase()
        require(normalized.isNotEmpty()) { "Empty secret" }
        val unpadded = normalized.trimEnd('=')
        require(unpadded.isNotEmpty()) { "Empty secret" }
        val bytes = ArrayList<Byte>()
        var buffer = 0
        var bitsLeft = 0
        unpadded.forEach { chr ->
            val value = BASE32_ALPHABET.indexOf(chr)
            require(value >= 0) { "Invalid Base32 secret" }
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            while (bitsLeft >= 8) {
                bitsLeft -= 8
                bytes.add(((buffer shr bitsLeft) and 0xff).toByte())
            }
        }
        return shellOf(bytes)
    }

    companion object {
        private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
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
