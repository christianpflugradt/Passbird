package de.pflugradts.passbird.domain.service

import de.pflugradts.passbird.domain.model.nest.Nest.Companion.DEFAULT
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.CAPACITY
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.service.password.storage.EggRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue
import strikt.java.isPresent
import kotlin.jvm.optionals.getOrNull

class NestingGroundServiceTest {

    private val eggRepository = mockk<EggRepository>(relaxed = true)
    private val nestingGroundService = NestingGroundService()

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
            expectThat(actual[it].get().shell) isEqualTo nestShells[it]
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
        expectThat(nestingGroundService.atNestSlot(NestSlot.DEFAULT).getOrNull()) isEqualTo DEFAULT
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
        val nest2 = nestingGroundService.atNestSlot(NestSlot.N2)
        expectThat(nest2).isPresent()
        expectThat(nest2.get().nestSlot) isEqualTo NestSlot.N2
        expectThat(nest2.get().shell) isEqualTo givenNestShell
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
        expectThat(nestingGroundService.atNestSlot(NestSlot.N1).isPresent).isFalse()
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
        expectThat(nestingGroundService.currentNest().nestSlot) isEqualTo NestSlot.DEFAULT
    }

    @Test
    fun `should update and return current nest`() {
        // given
        val nestShells = listOf(
            emptyShell(), shellOf("nestSlot2"), emptyShell(), emptyShell(),
            emptyShell(), emptyShell(), emptyShell(), emptyShell(), emptyShell(),
        )
        nestingGroundService.populate(nestShells)
        val wantedCurrentNestSlot = NestSlot.N2

        // when
        nestingGroundService.moveToNestAt(wantedCurrentNestSlot)

        // then
        expectThat(nestingGroundService.currentNest().nestSlot) isEqualTo wantedCurrentNestSlot
    }

    @Test
    fun `should not update anything if nest is not deployed`() {
        // given
        val nestShells = listOf(
            emptyShell(), shellOf("nestSlot2"), emptyShell(), emptyShell(),
            emptyShell(), emptyShell(), emptyShell(), emptyShell(), emptyShell(),
        )
        nestingGroundService.populate(nestShells)
        val wantedCurrentNestSlot = NestSlot.N1

        // when
        nestingGroundService.moveToNestAt(wantedCurrentNestSlot)

        // then
        expectThat(nestingGroundService.currentNest().nestSlot) isNotEqualTo wantedCurrentNestSlot
    }

    @Disabled // FIXME needs to be resolved without creating a circular dependency
    @Test
    fun `should deploy nest and sync`() {
        // given
        val nestShell = shellOf("Nest")

        // when
        nestingGroundService.place(nestShell, NestSlot.N3)
        val actual = nestingGroundService.atNestSlot(NestSlot.N3)

        // then
        expectThat(actual.isPresent).isTrue()
        expectThat(actual.get().shell) isEqualTo nestShell
        expectThat(actual.get().nestSlot) isEqualTo NestSlot.N3
        verify(exactly = 1) { eggRepository.sync() }
    }
}
