package de.pflugradts.passbird.application.passwordtree

import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.security.createAesGcmCipherForTesting
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.copyInt
import de.pflugradts.passbird.application.util.scramble
import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private const val PERSISTED_SLOT_ENTRIES = (Slot.CAPACITY + 1) * (Slot.CAPACITY + 1)

class PasswordTreePayloadReaderMalformedPayloadTest {

    private val configuration = mockk<Configuration>()
    private val systemOperation = mockk<SystemOperation>(relaxed = true)
    private val cryptoProvider = createAesGcmCipherForTesting()

    @BeforeEach
    fun setup() {
        fakeConfiguration(instance = configuration)
    }

    @Test
    fun `current reader rejects invalid egg nest slots`() {
        val payload = currentPayloadBytes()
        val malformed = payload.withPayloadInt(eggOffsets(payload, currentEggOffset(payload), hasTrashMetadata = true).nestSlot, 99)

        assertMalformedCurrent(malformed)
    }

    @Test
    fun `legacy reader rejects invalid egg nest slots`() {
        val payload = legacyPayloadBytes()
        val malformed = payload.withPayloadInt(eggOffsets(payload, legacyEggOffset(payload), hasTrashMetadata = false).nestSlot, 99)

        assertMalformedLegacy(malformed)
    }

    @Test
    fun `current reader rejects negative size fields`() {
        val payload = currentPayloadBytes()

        currentSizeOffsets(payload).forEach {
            assertMalformedCurrent(payload.withPayloadInt(it, -1))
        }
    }

    @Test
    fun `current reader rejects overlong size fields`() {
        val payload = currentPayloadBytes()

        currentSizeOffsets(payload).forEach {
            assertMalformedCurrent(payload.withPayloadInt(it, payload.size))
        }
    }

    @Test
    fun `legacy reader rejects negative size fields`() {
        val payload = legacyPayloadBytes()

        legacySizeOffsets(payload).forEach {
            assertMalformedLegacy(payload.withPayloadInt(it, -1))
        }
    }

    @Test
    fun `legacy reader rejects overlong size fields`() {
        val payload = legacyPayloadBytes()

        legacySizeOffsets(payload).forEach {
            assertMalformedLegacy(payload.withPayloadInt(it, payload.size))
        }
    }

    @Test
    fun `current reader rejects trailing partial egg records`() {
        assertMalformedCurrent(currentPayloadBytes(PasswordTreeSnapshot()).withTrailingPartialRecord())
    }

    @Test
    fun `legacy reader rejects trailing partial egg records`() {
        assertMalformedLegacy(legacyPayloadBytes(PasswordTreeSnapshot()).withTrailingPartialRecord())
    }

    private fun assertMalformedCurrent(payload: ByteArray) {
        assertThrows<IllegalStateException> {
            PasswordTreePayloadReader(configuration, systemOperation).read(shellOf(payload))
        }
    }

    private fun assertMalformedLegacy(payload: ByteArray) {
        assertThrows<IllegalStateException> {
            LegacyPasswordTreePayloadReader(configuration, systemOperation).read(shellOf(payload))
        }
    }

    private fun currentPayloadBytes(snapshot: PasswordTreeSnapshot = snapshotWithEgg()) =
        PasswordTreePayloadWriter().write(snapshot).toByteArray()

    private fun legacyPayloadBytes(snapshot: PasswordTreeSnapshot = snapshotWithEgg()) =
        LegacyPasswordTreePayloadWriter().write(snapshot).toByteArray()

    private fun snapshotWithEgg() = PasswordTreeSnapshot(
        eggs = listOf(
            createEgg(
                slot = Slot.S1,
                eggIdShell = cryptoProvider.encrypt(shellOf("email")),
                passwordShell = cryptoProvider.encrypt(shellOf("Password1")),
            ),
        ),
    )

    private fun currentSizeOffsets(payload: ByteArray): List<Int> {
        val eggOffsets = eggOffsets(payload, currentEggOffset(payload), hasTrashMetadata = true)
        return listOf(
            signatureSize() + 2 * Integer.BYTES,
            currentFavoriteOffset(payload),
            currentNestOffset(payload),
            eggOffsets.eggIdSize,
            eggOffsets.passwordSize,
            eggOffsets.firstProteinTypeSize,
            eggOffsets.firstProteinStructureSize,
        )
    }

    private fun legacySizeOffsets(payload: ByteArray): List<Int> {
        val eggOffsets = eggOffsets(payload, legacyEggOffset(payload), hasTrashMetadata = false)
        return listOf(
            signatureSize(),
            legacyNestOffset(payload),
            eggOffsets.eggIdSize,
            eggOffsets.passwordSize,
            eggOffsets.firstProteinTypeSize,
            eggOffsets.firstProteinStructureSize,
        )
    }

    private fun currentFavoriteOffset(payload: ByteArray) = skipEntries(
        payload,
        signatureSize() + 2 * Integer.BYTES,
        PERSISTED_SLOT_ENTRIES,
    )

    private fun currentNestOffset(payload: ByteArray) = skipEntries(payload, currentFavoriteOffset(payload), PERSISTED_SLOT_ENTRIES)

    private fun currentEggOffset(payload: ByteArray) = skipEntries(payload, currentNestOffset(payload), Slot.CAPACITY)

    private fun legacyNestOffset(payload: ByteArray) = skipEntries(payload, signatureSize(), PERSISTED_SLOT_ENTRIES)

    private fun legacyEggOffset(payload: ByteArray) = skipEntries(payload, legacyNestOffset(payload), Slot.CAPACITY)

    private fun skipEntries(payload: ByteArray, offset: Int, entries: Int): Int {
        var incrementedOffset = offset
        repeat(entries) {
            incrementedOffset += Integer.BYTES + readTestInt(payload, incrementedOffset)
        }
        return incrementedOffset
    }

    private fun eggOffsets(payload: ByteArray, offset: Int, hasTrashMetadata: Boolean): EggOffsets {
        var incrementedOffset = offset
        val nestSlot = incrementedOffset
        incrementedOffset += Integer.BYTES
        if (hasTrashMetadata) {
            incrementedOffset += 2 * Integer.BYTES
        }
        val eggIdSize = incrementedOffset
        incrementedOffset += Integer.BYTES + readTestInt(payload, incrementedOffset)
        val passwordSize = incrementedOffset
        incrementedOffset += Integer.BYTES + readTestInt(payload, incrementedOffset)
        val firstProteinTypeSize = incrementedOffset
        incrementedOffset += Integer.BYTES + readTestInt(payload, incrementedOffset)
        return EggOffsets(
            nestSlot = nestSlot,
            eggIdSize = eggIdSize,
            passwordSize = passwordSize,
            firstProteinTypeSize = firstProteinTypeSize,
            firstProteinStructureSize = incrementedOffset,
        )
    }

    private fun ByteArray.withPayloadInt(offset: Int, value: Int) = copyOf().also {
        copyInt(value, it, offset)
        it.refreshChecksum()
    }

    private fun ByteArray.withTrailingPartialRecord() =
        (copyOfRange(0, size - checksumBytes()) + byteArrayOf(1) + copyOfRange(size - checksumBytes(), size)).also {
            it.refreshChecksum()
        }

    private fun ByteArray.refreshChecksum() {
        val checksumSource = copyOfRange(signatureSize(), size - checksumBytes())
        try {
            this[size - checksumBytes()] = checksum(checksumSource)
        } finally {
            checksumSource.scramble()
        }
    }

    private fun readTestInt(bytes: ByteArray, offset: Int) = ((bytes[offset].toInt() and 0xFF) shl 24) or
        ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
        ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
        (bytes[offset + 3].toInt() and 0xFF)

    private data class EggOffsets(
        val nestSlot: Int,
        val eggIdSize: Int,
        val passwordSize: Int,
        val firstProteinTypeSize: Int,
        val firstProteinStructureSize: Int,
    )
}
