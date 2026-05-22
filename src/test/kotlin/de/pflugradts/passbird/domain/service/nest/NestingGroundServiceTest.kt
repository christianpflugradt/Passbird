package de.pflugradts.passbird.domain.service.nest

import de.pflugradts.passbird.domain.model.nest.Nest.Companion.DEFAULT
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.CAPACITY
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.tree.EggRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue

class NestingGroundServiceTest {

    private val eggRepository = mockk<EggRepository>(relaxed = true)
    private val eventRegistry = mockk<EventRegistry>(relaxed = true)
    private val nestingGroundService = NestingGroundService(eggRepository, eventRegistry)

    @Test
    fun `should have 9 empty nest slots upon initialisation`() {
        // given / when
        val actual = nestingGroundService.all().toList()

        // then
        expectThat(actual) hasSize CAPACITY
        expectThat(actual.stream().allMatch { it.isEmpty }).isTrue()
    }

    @Test
    fun `should populate nests`() {
        // given
        val nestShells = listOf(
            emptyShell(), shellOf("nest1"), emptyShell(), shellOf("nest3"),
            emptyShell(), emptyShell(), emptyShell(), shellOf("nest7"), emptyShell(),
        )

        // when
        nestingGroundService.populate(nestShells)
        val actual = nestingGroundService.all().toList()

        // then
        intArrayOf(1, 3, 7).forEach {
            expectThat(actual[it].isPresent).isTrue()
            expectThat(actual[it].get().viewNestId()) isEqualTo nestShells[it]
        }
        intArrayOf(0, 2, 4, 5, 6, 8).forEach { expectThat(actual[it].isPresent).isFalse() }
    }

    @Test
    fun `should not populate nests if number of nests does not match`() {
        // given
        val nestShells = listOf(shellOf("nest1"), shellOf("nest2"), shellOf("nest3"))

        // when
        nestingGroundService.populate(nestShells)
        val actual = nestingGroundService.all().toList()

        // then
        (0..<9).forEach {
            expectThat(actual[it].isEmpty).isTrue()
        }
    }

    @Test
    fun `should return default nest for default nest slot`() {
        // given / when / then
        expectThat(nestingGroundService.atNestSlot(Slot.DEFAULT).orNull()) isEqualTo DEFAULT
    }

    @Test
    fun `should return nest for non empty nest slot`() {
        // given
        val givenNestShell = shellOf("nestSlot2")
        val nestShells = listOf(
            emptyShell(), givenNestShell, emptyShell(), emptyShell(),
            emptyShell(), emptyShell(), emptyShell(), emptyShell(), emptyShell(),
        )

        // when
        nestingGroundService.populate(nestShells)

        // then
        val nest2 = nestingGroundService.atNestSlot(Slot.S2)
        expectThat(nest2.isPresent).isTrue()
        expectThat(nest2.get().slot) isEqualTo Slot.S2
        expectThat(nest2.get().viewNestId()) isEqualTo givenNestShell
    }

    @Test
    fun `should return empty optional for empty nest slot`() {
        // given
        val nestShells = listOf(
            emptyShell(), shellOf("nestSlot2"), emptyShell(), emptyShell(),
            emptyShell(), emptyShell(), emptyShell(), emptyShell(), emptyShell(),
        )

        // when
        nestingGroundService.populate(nestShells)

        // then
        expectThat(nestingGroundService.atNestSlot(Slot.S1).isPresent).isFalse()
    }

    @Test
    fun `should return default nest if none is set`() {
        // given
        val nestShells = listOf(
            emptyShell(), shellOf("nestSlot2"), emptyShell(), emptyShell(),
            emptyShell(), emptyShell(), emptyShell(), emptyShell(), emptyShell(),
        )

        // when
        nestingGroundService.populate(nestShells)

        // then
        expectThat(nestingGroundService.currentNest().slot) isEqualTo Slot.DEFAULT
    }

    @Test
    fun `should update and return current nest`() {
        // given
        val nestShells = listOf(
            emptyShell(), shellOf("nestSlot2"), emptyShell(), emptyShell(),
            emptyShell(), emptyShell(), emptyShell(), emptyShell(), emptyShell(),
        )
        nestingGroundService.populate(nestShells)
        val wantedCurrentSlot = Slot.S2

        // when
        nestingGroundService.moveToNestAt(wantedCurrentSlot)

        // then
        expectThat(nestingGroundService.currentNest().slot) isEqualTo wantedCurrentSlot
    }

    @Test
    fun `should not update anything if nest is not deployed`() {
        // given
        val nestShells = listOf(
            emptyShell(), shellOf("nestSlot2"), emptyShell(), emptyShell(),
            emptyShell(), emptyShell(), emptyShell(), emptyShell(), emptyShell(),
        )
        nestingGroundService.populate(nestShells)
        val wantedCurrentSlot = Slot.S1

        // when
        nestingGroundService.moveToNestAt(wantedCurrentSlot)

        // then
        expectThat(nestingGroundService.currentNest().slot) isNotEqualTo wantedCurrentSlot
    }

    @Test
    fun `should deploy nest and sync`() {
        // given
        val nestShell = shellOf("Nest")

        // when
        nestingGroundService.place(nestShell, Slot.S3)
        val actual = nestingGroundService.atNestSlot(Slot.S3)

        // then
        expectThat(actual.isPresent).isTrue()
        expectThat(actual.get().viewNestId()) isEqualTo nestShell
        expectThat(actual.get().slot) isEqualTo Slot.S3
        verify(exactly = 1) { eggRepository.sync() }
    }
}
