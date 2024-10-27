package de.pflugradts.passbird.domain.model.shell

import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.domain.model.shell.EncryptedShell.Companion.encryptedShellOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isNotSameInstanceAs
import strikt.assertions.isTrue

private const val IV_SIZE = 12

class EncryptedShellTest {

    @Test
    fun `should have correct size`() {
        // given
        val givenPayload = byteArrayOf(1, 2, 3)
        val givenEncryptedShell = encryptedShellOf(ivAndPayload(givenPayload))
        val expectedSize = IV_SIZE + 3

        // when
        val actual = givenEncryptedShell.size

        // then
        expectThat(actual) isEqualTo expectedSize
    }

    @Test
    fun `should copy encrypted shell`() {
        // given
        val givenPayload = byteArrayOf(1, 2, 3)
        val givenEncryptedShell = encryptedShellOf(ivAndPayload(givenPayload))

        // when
        val actual = givenEncryptedShell.copy()

        // then
        expectThat(actual) isEqualTo givenEncryptedShell isNotSameInstanceAs givenEncryptedShell
    }

    @Test
    fun `should scramble iv and payload`() {
        // given
        val givenPayload = byteArrayOf(1, 2, 3)
        val encryptedShell = encryptedShellOf(ivAndPayload(givenPayload))
        val givenIv = encryptedShell.iv.toByteArray()
        val givenSize = encryptedShell.size

        // when
        expectThat(encryptedShell.iv.toByteArray()) isEqualTo givenIv
        expectThat(encryptedShell.payload.toByteArray()) isEqualTo givenPayload
        encryptedShell.scramble()

        // then
        expectThat(encryptedShell.size) isEqualTo givenSize
        expectThat(encryptedShell.iv.toByteArray()) isNotEqualTo givenIv
        expectThat(encryptedShell.payload.toByteArray()) isNotEqualTo givenPayload
    }

    @Nested
    inner class InstantiationTest {
        @Test
        fun `should instantiate from byte array`() {
            // given
            val givenIvBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6)
            val givenPayloadBytes = byteArrayOf(7, 8, 9)
            val givenBytes = givenIvBytes + givenPayloadBytes

            // when
            val actual = encryptedShellOf(givenBytes)

            // then
            expectThat(actual.iv.toByteArray()) isEqualTo givenIvBytes
            expectThat(actual.payload.toByteArray()) isEqualTo givenPayloadBytes
        }

        @Test
        fun `should instantiate from byte array with empty payload`() {
            // given
            val givenIvBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6)
            val givenPayloadBytes = byteArrayOf()
            val givenBytes = givenIvBytes + givenPayloadBytes

            // when
            val actual = encryptedShellOf(givenBytes)

            // then
            expectThat(actual.iv.toByteArray()) isEqualTo givenIvBytes
            expectThat(actual.payload.isEmpty).isTrue()
        }

        @Test
        fun `should fail on byte array with size less than expected iv size`() {
            // given
            val givenIvBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5)
            val givenPayloadBytes = byteArrayOf()
            val givenBytes = givenIvBytes + givenPayloadBytes

            // when
            val actual = tryCatching { encryptedShellOf(givenBytes) }

            // then
            expectThat(actual.failure).isTrue()
        }
    }

    @Nested
    inner class EqualsTest {

        @Test
        fun `should be equal to itself`() {
            // given
            val encryptedShell = encryptedShellOf(ivAndPayload(byteArrayOf(1, 2, 3)))

            // when
            val actual = encryptedShell.equals(encryptedShell)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should be equal to encrypted shell with equal content`() {
            // given
            val content = ivAndPayload(byteArrayOf(1, 2, 3))
            val encryptedShell1 = encryptedShellOf(content)
            val encryptedShell2 = encryptedShellOf(content)

            // when
            val actual = encryptedShell1.equals(encryptedShell2)

            // then
            expectThat(actual).isTrue()
        }

        @Test
        fun `should not be equal to encrypted shell with other content`() {
            // given
            val content = ivAndPayload(byteArrayOf(1, 2, 3))
            val otherContent = ivAndPayload(byteArrayOf(1, 2, 4))
            val encryptedShell1 = encryptedShellOf(content)
            val encryptedShell2 = encryptedShellOf(otherContent)

            // when
            val actual = encryptedShell1.equals(encryptedShell2)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to other class`() {
            // given
            val content = ivAndPayload(byteArrayOf(1, 2, 3))
            val encryptedShell = encryptedShellOf(content)

            // when
            val actual = encryptedShell.equals(content)

            // then
            expectThat(actual).isFalse()
        }

        @Test
        fun `should not be equal to null`() {
            // given
            val encryptedShell = encryptedShellOf(ivAndPayload(byteArrayOf(1, 2, 3)))

            // when
            val actual = encryptedShell.equals(null)

            // then
            expectThat(actual).isFalse()
        }
    }
}

private fun ivAndPayload(payload: ByteArray) = ByteArray(IV_SIZE) + payload
