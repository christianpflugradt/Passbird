package de.pflugradts.passbird.application.yolk

import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class TotpSupportTest {
    @Test
    fun `should parse otpauth uri with defaults and generate expected code`() {
        val parsed = YolkInputParser().parse(shellOf("otpauth://totp/Passbird:egg?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"))
        val actual = TotpGenerator(Clock.fixed(Instant.parse("1970-01-01T00:00:59Z"), ZoneOffset.UTC)).generate(
            secret = parsed.secret.toByteArray(),
            algorithm = parsed.algorithm,
            digits = parsed.digits,
            periodSeconds = parsed.periodSeconds,
        )

        expectThat(parsed.algorithm) isEqualTo "SHA1"
        expectThat(parsed.digits) isEqualTo 6
        expectThat(parsed.periodSeconds) isEqualTo 30
        expectThat(actual.value) isEqualTo "287082"
        expectThat(actual.remainingValiditySeconds) isEqualTo 1
    }

    @Test
    fun `should parse base32 secret and custom otpauth parameters`() {
        val base32 = YolkInputParser().parse(shellOf("MZXW6YTB"))
        val uri = YolkInputParser().parse(
            shellOf("otpauth://totp/Passbird:egg?secret=MZXW6YTB&algorithm=SHA512&digits=8&period=45"),
        )

        expectThat(base32.secret.asString()) isEqualTo "fooba"
        expectThat(uri.secret.asString()) isEqualTo "fooba"
        expectThat(uri.algorithm) isEqualTo "SHA512"
        expectThat(uri.digits) isEqualTo 8
        expectThat(uri.periodSeconds) isEqualTo 45
    }

    @Test
    fun `should accept grouped base32 secrets with whitespace hyphens and underscores`() {
        val whitespace = YolkInputParser().parse(shellOf("MZXW 6YTB"))
        val hyphen = YolkInputParser().parse(shellOf("MZXW-6YTB"))
        val underscore = YolkInputParser().parse(shellOf("MZXW_6YTB"))
        val lowercase = YolkInputParser().parse(shellOf("mzxw 6ytb"))

        expectThat(whitespace.secret.asString()) isEqualTo "fooba"
        expectThat(hyphen.secret.asString()) isEqualTo "fooba"
        expectThat(underscore.secret.asString()) isEqualTo "fooba"
        expectThat(lowercase.secret.asString()) isEqualTo "fooba"
    }

    @Test
    fun `should accept grouped secret in otpauth uri`() {
        val parsed = YolkInputParser().parse(
            shellOf("otpauth://totp/Passbird:egg?secret=MZXW-6YTB&algorithm=SHA512&digits=8&period=45"),
        )

        expectThat(parsed.secret.asString()) isEqualTo "fooba"
        expectThat(parsed.algorithm) isEqualTo "SHA512"
        expectThat(parsed.digits) isEqualTo 8
        expectThat(parsed.periodSeconds) isEqualTo 45
    }

    @Test
    fun `should use configured defaults when parsing plain secret`() {
        val parsed = YolkInputParser().parse(
            shellOf("MZXW6YTB"),
            ConfiguredYolkDefaults(
                algorithm = "SHA256",
                digits = 8,
                periodSeconds = 45,
            ),
        )

        expectThat(parsed.secret.asString()) isEqualTo "fooba"
        expectThat(parsed.algorithm) isEqualTo "SHA256"
        expectThat(parsed.digits) isEqualTo 8
        expectThat(parsed.periodSeconds) isEqualTo 45
    }

    @Test
    fun `should decode encoded otpauth parameters and accept lowercase algorithms`() {
        val parsed = YolkInputParser().parse(
            shellOf("otpauth://totp/Passbird%3Aegg?secret=MZXW6YTBOI======&algorithm=sha256&digits=8&period=60&issuer=Passbird"),
        )

        expectThat(parsed.secret.asString()) isEqualTo "foobar"
        expectThat(parsed.algorithm) isEqualTo "SHA256"
        expectThat(parsed.digits) isEqualTo 8
        expectThat(parsed.periodSeconds) isEqualTo 60
    }

    @Test
    fun `should use full period as remaining validity when code changes exactly at boundary`() {
        val actual = TotpGenerator(Clock.fixed(Instant.parse("1970-01-01T00:01:00Z"), ZoneOffset.UTC)).generate(
            secret = shellOf("12345678901234567890").toByteArray(),
            algorithm = "SHA1",
            digits = 6,
            periodSeconds = 30,
        )

        expectThat(actual.remainingValiditySeconds) isEqualTo 30
    }

    @Test
    fun `should generate codes for non default algorithms and digits`() {
        val actual = TotpGenerator(Clock.fixed(Instant.parse("1970-01-01T00:01:29Z"), ZoneOffset.UTC)).generate(
            secret = shellOf("12345678901234567890123456789012").toByteArray(),
            algorithm = "SHA256",
            digits = 8,
            periodSeconds = 30,
        )

        expectThat(actual.value) isEqualTo "30882438"
        expectThat(actual.remainingValiditySeconds) isEqualTo 1
    }

    @Test
    fun `should expose totp algorithm helper roundtrip`() {
        expectThat(defaultTotpAlgorithm()) isEqualTo "SHA1"
        expectThat(normalizeTotpAlgorithm("sha512")) isEqualTo "SHA512"
        expectThat(totpAlgorithmOrdinal("SHA256")) isEqualTo 1
        expectThat(totpAlgorithmAtOrdinal(2)) isEqualTo "SHA512"
    }

    @Test
    fun `should reject malformed yolk parser helper parameters`() {
        assertThrows<IllegalArgumentException> { YolkInputParser().parse(shellOf("otpauth://totp/Passbird:egg?secret=")) }
        assertThrows<IllegalArgumentException> {
            YolkInputParser().parse(shellOf("otpauth://totp/Passbird:egg?secret=MZXW6YTB&algorithm=MD5"))
        }
        assertThrows<IllegalArgumentException> { YolkInputParser().parse(shellOf("otpauth://totp/%ZZ?secret=MZXW6YTB")) }
        assertThrows<IllegalArgumentException> { YolkInputParser().parse(shellOf(" \n\t ")) }
        assertThrows<IllegalArgumentException> { YolkInputParser().parse(shellOf("MZXW6Y!B")) }
        assertThrows<IllegalArgumentException> {
            YolkInputParser().parse(
                shellOf("MZXW6YTB"),
                ConfiguredYolkDefaults(
                    algorithm = "MD5",
                    digits = 6,
                    periodSeconds = 30,
                ),
            )
        }
        assertThrows<IllegalStateException> { totpAlgorithmAtOrdinal(99) }
        assertThrows<IllegalArgumentException> { YolkInputParser().parse(shellOf("MZXW.6YTB")) }
    }

    @Test
    fun `should reject invalid yolk parser and generator parameters`() {
        assertThrows<IllegalArgumentException> { YolkInputParser().parse(shellOf("otpauth://totp/Passbird:egg?secret=MZXW6YTB&digits=7")) }
        assertThrows<IllegalArgumentException> { YolkInputParser().parse(shellOf("otpauth://totp/Passbird:egg?secret=MZXW6YTB&period=0")) }
        assertThrows<IllegalArgumentException> { YolkInputParser().parse(shellOf("otpauth://hotp/Passbird:egg?secret=MZXW6YTB")) }
        assertThrows<IllegalArgumentException> {
            TotpGenerator(Clock.systemUTC()).generate(
                secret = shellOf("12345678901234567890").toByteArray(),
                algorithm = "SHA1",
                digits = 7,
                periodSeconds = 30,
            )
        }
        assertThrows<IllegalArgumentException> {
            TotpGenerator(Clock.systemUTC()).generate(
                secret = shellOf("12345678901234567890").toByteArray(),
                algorithm = "MD5",
                digits = 6,
                periodSeconds = 30,
            )
        }
        assertThrows<IllegalArgumentException> {
            TotpGenerator(Clock.systemUTC()).generate(
                secret = shellOf("12345678901234567890").toByteArray(),
                algorithm = "SHA1",
                digits = 6,
                periodSeconds = 0,
            )
        }
    }
}
