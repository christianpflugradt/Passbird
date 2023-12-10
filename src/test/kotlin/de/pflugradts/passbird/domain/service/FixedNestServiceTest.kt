package de.pflugradts.passbird.domain.service

import de.pflugradts.passbird.domain.model.nest.Nest.Companion.DEFAULT
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.nest.Slot.Companion.CAPACITY
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
import de.pflugradts.passbird.domain.service.password.storage.PasswordEntryRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue
import strikt.java.isPresent
import kotlin.jvm.optionals.getOrNull

class FixedNestServiceTest {

    private val passwordEntryRepository = mockk<PasswordEntryRepository>(relaxed = true)
    private val nestService = FixedNestService(passwordEntryRepository)

    @Test
    fun `should have 9 empty slots upon initialisation`() {
        // given / when
        val actual = nestService.all().toList()

        // then
        expectThat(actual) hasSize CAPACITY
        expectThat(actual.stream().allMatch { it.isEmpty }).isTrue()
    }

    @Test
    fun `should populate nests`() {
        // given
        val nestBytes = listOf(
            emptyBytes(), bytesOf("nest1"), emptyBytes(), bytesOf("nest3"),
            emptyBytes(), emptyBytes(), emptyBytes(), bytesOf("nest7"), emptyBytes(),
        )

        // when
        nestService.populate(nestBytes)
        val actual = nestService.all().toList()

        // then
        intArrayOf(1, 3, 7).forEach {
            expectThat(actual[it].isPresent).isTrue()
            expectThat(actual[it].get().bytes) isEqualTo nestBytes[it]
        }
        intArrayOf(0, 2, 4, 5, 6, 8).forEach { expectThat(actual[it].isPresent).isFalse() }
    }

    @Test
    fun `should not populate nests if number of nests does not match`() {
        // given
        val nestBytes = listOf(bytesOf("nest1"), bytesOf("nest2"), bytesOf("nest3"))

        // when
        nestService.populate(nestBytes)
        val actual = nestService.all().toList()

        // then
        (0..<9).forEach {
            expectThat(actual[it].isEmpty).isTrue()
        }
    }

    @Test
    fun `should return default nest for default slot`() {
        // given / when / then
        expectThat(nestService.atSlot(Slot.DEFAULT).getOrNull()) isEqualTo DEFAULT
    }

    @Test
    fun `should return nest for non empty slot`() {
        // given
        val givenNestBytes = bytesOf("slot2")
        val nestBytes = listOf(
            emptyBytes(), givenNestBytes, emptyBytes(), emptyBytes(),
            emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(),
        )

        // when
        nestService.populate(nestBytes)

        // then
        val nest2 = nestService.atSlot(Slot.N2)
        expectThat(nest2).isPresent()
        expectThat(nest2.get().slot) isEqualTo Slot.N2
        expectThat(nest2.get().bytes) isEqualTo givenNestBytes
    }

    @Test
    fun `should return empty optional for empty slot`() {
        // given
        val nestBytes = listOf(
            emptyBytes(), bytesOf("slot2"), emptyBytes(), emptyBytes(),
            emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(),
        )

        // when
        nestService.populate(nestBytes)

        // then
        expectThat(nestService.atSlot(Slot.N1).isPresent).isFalse()
    }

    @Test
    fun `should return default nest if none is set`() {
        // given
        val nestBytes = listOf(
            emptyBytes(), bytesOf("slot2"), emptyBytes(), emptyBytes(),
            emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(),
        )

        // when
        nestService.populate(nestBytes)

        // then
        expectThat(nestService.getCurrentNest().slot) isEqualTo Slot.DEFAULT
    }

    @Test
    fun `should update and return current nest`() {
        // given
        val nestBytes = listOf(
            emptyBytes(), bytesOf("slot2"), emptyBytes(), emptyBytes(),
            emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(),
        )
        nestService.populate(nestBytes)
        val wantedCurrentNestSlot = Slot.N2

        // when
        nestService.moveToNestAt(wantedCurrentNestSlot)

        // then
        expectThat(nestService.getCurrentNest().slot) isEqualTo wantedCurrentNestSlot
    }

    @Test
    fun `should not update anything if nest is not deployed`() {
        // given
        val nestBytes = listOf(
            emptyBytes(), bytesOf("slot2"), emptyBytes(), emptyBytes(),
            emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(), emptyBytes(),
        )
        nestService.populate(nestBytes)
        val wantedCurrentNestSlot = Slot.N1

        // when
        nestService.moveToNestAt(wantedCurrentNestSlot)

        // then
        expectThat(nestService.getCurrentNest().slot) isNotEqualTo wantedCurrentNestSlot
    }

    @Test
    fun `should deploy nest and sync`() {
        // given
        val nestBytes = bytesOf("name space")

        // when
        nestService.deploy(nestBytes, Slot.N3)
        val actual = nestService.atSlot(Slot.N3)

        // then
        expectThat(actual.isPresent).isTrue()
        expectThat(actual.get().bytes) isEqualTo nestBytes
        expectThat(actual.get().slot) isEqualTo Slot.N3
        verify(exactly = 1) { passwordEntryRepository.sync() }
    }
}
