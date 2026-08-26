package de.pflugradts.passbird.application.passwordtree

import de.pflugradts.passbird.domain.model.slot.Slot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class PasswordTreePayloadValidationTest {

    @Test
    fun `should accept payload envelope at minimum supported size`() {
        validatePayloadEnvelope(ByteArray(signatureSize() + checksumBytes()))
    }

    @Test
    fun `should reject payload envelope smaller than signature and checksum`() {
        assertThrows<IllegalStateException> {
            validatePayloadEnvelope(ByteArray(signatureSize() + checksumBytes() - 1))
        }
    }

    @Test
    fun `should calculate payload content end before checksum`() {
        expectThat(payloadContentEnd(ByteArray(17))) isEqualTo 16
    }

    @Test
    fun `should read payload int as big endian`() {
        val bytes = byteArrayOf(0x01, 0x23, 0x45, 0x67)

        expectThat(readPayloadInt(bytes, 0)) isEqualTo 0x01234567
    }

    @Test
    fun `should reject reading payload int beyond exclusive limit`() {
        val bytes = byteArrayOf(0x01, 0x23, 0x45, 0x67)

        assertThrows<IllegalStateException> {
            readPayloadInt(bytes, 1, bytes.size)
        }
    }

    @Test
    fun `should read payload size when non negative`() {
        expectThat(readPayloadSize(byteArrayOf(0x00, 0x00, 0x00, 0x02), 0)) isEqualTo 2
    }

    @Test
    fun `should reject negative payload size`() {
        assertThrows<IllegalStateException> {
            readPayloadSize(byteArrayOf(-1, -1, -1, -1), 0)
        }
    }

    @Test
    fun `should read payload bytes within range`() {
        val bytes = byteArrayOf(9, 8, 7, 6, 5)

        expectThat(readPayloadBytes(bytes, 1, 3)) isEqualTo byteArrayOf(8, 7, 6)
    }

    @Test
    fun `should reject payload bytes when requested range exceeds limit`() {
        val bytes = byteArrayOf(9, 8, 7, 6, 5)

        assertThrows<IllegalStateException> {
            readPayloadBytes(bytes, 3, 3, bytes.size)
        }
    }

    @Test
    fun `should accept payload range that ends exactly at exclusive limit`() {
        validatePayloadRange(byteArrayOf(1, 2, 3, 4), offset = 1, size = 3, limitExclusive = 4)
    }

    @Test
    fun `should reject payload range with invalid bounds`() {
        val bytes = byteArrayOf(1, 2, 3, 4)

        listOf(
            { validatePayloadRange(bytes, offset = -1, size = 1) },
            { validatePayloadRange(bytes, offset = 0, size = -1) },
            { validatePayloadRange(bytes, offset = 0, size = 1, limitExclusive = -1) },
            { validatePayloadRange(bytes, offset = 0, size = 1, limitExclusive = bytes.size + 1) },
            { validatePayloadRange(bytes, offset = 3, size = 2, limitExclusive = bytes.size) },
        ).forEach { invalidCall ->
            assertThrows<IllegalStateException> {
                invalidCall()
            }
        }
    }

    @Test
    fun `should accept matching payload end`() {
        validatePayloadEnd(offset = 4, endExclusive = 4)
    }

    @Test
    fun `should reject mismatching payload end`() {
        assertThrows<IllegalStateException> {
            validatePayloadEnd(offset = 4, endExclusive = 5)
        }
    }

    @Test
    fun `should resolve persisted default and numbered egg nest slots`() {
        expectThat(storedEggNestSlot(Slot.DEFAULT.index())) isEqualTo Slot.DEFAULT
        expectThat(storedEggNestSlot(Slot.S1.index())) isEqualTo Slot.S1
        expectThat(storedEggNestSlot(Slot.S9.index())) isEqualTo Slot.S9
    }

    @Test
    fun `should reject unsupported persisted egg nest slot`() {
        assertThrows<IllegalStateException> {
            storedEggNestSlot(10)
        }
    }
}
