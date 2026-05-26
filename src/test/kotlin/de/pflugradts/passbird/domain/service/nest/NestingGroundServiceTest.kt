package de.pflugradts.passbird.domain.service.nest

import de.pflugradts.passbird.domain.model.nest.Nest.Companion.DEFAULT
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.CAPACITY
import de.pflugradts.passbird.domain.service.eventhandling.EventRegistry
import de.pflugradts.passbird.domain.service.password.tree.EggStreamSupplier
import de.pflugradts.passbird.domain.service.password.tree.PasswordTreeAdapterPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue
import java.util.stream.Stream

class NestingGroundServiceTest {

    private val restoredNests = listOf(
        emptyShell(), shellOf("restored1"), emptyShell(), shellOf("restored3"),
        emptyShell(), emptyShell(), emptyShell(), shellOf("restored7"), emptyShell(),
    )
    private val passwordTreeAdapterPort = mockk<PasswordTreeAdapterPort>(relaxed = true).also {
        every { it.restore() } returns EggStreamSupplier({ Stream.empty() }, nests = restoredNests)
    }
    private val eventRegistry = mockk<EventRegistry>(relaxed = true)
    private val nestingGroundService = NestingGroundService(passwordTreeAdapterPort, eventRegistry)

    @Test
    fun `should have 9 empty nest slots upon initialisation`() {
        // given / when
        val actual = NestingGroundService(mockk(relaxed = true), eventRegistry).populateAndList(emptyList())

        // then
        expectThat(actual) hasSize CAPACITY
        expectThat(actual.stream().allMatch { it.isEmpty }).isTrue()
    }

    @Test
    fun `should restore nests from password tree on first read without syncing`() {
        // given / when
        val actual = nestingGroundService.all().toList()

        // then
        intArrayOf(1, 3, 7).forEach {
            expectThat(actual[it].isPresent).isTrue()
            expectThat(actual[it].get().viewNestId()) isEqualTo restoredNests[it]
        }
        intArrayOf(0, 2, 4, 5, 6, 8).forEach { expectThat(actual[it].isPresent).isFalse() }
        verify(exactly = 1) { passwordTreeAdapterPort.restore() }
        verify(exactly = 0) { passwordTreeAdapterPort.sync(any()) }
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
        (0..<CAPACITY).forEach {
            expectThat(actual[it].isEmpty).isTrue()
        }
        verify(exactly = 0) { passwordTreeAdapterPort.restore() }
    }

    @Test
    fun `should return default nest for default nest slot`() {
        // given / when / then
        expectThat(nestingGroundService.atNestSlot(Slot.DEFAULT).orNull()) isEqualTo DEFAULT
    }

    @Test
    fun `should return nest for non empty nest slot`() {
        // given / when
        val nest2 = nestingGroundService.atNestSlot(Slot.S2)

        // then
        expectThat(nest2.isPresent).isTrue()
        expectThat(nest2.get().slot) isEqualTo Slot.S2
        expectThat(nest2.get().viewNestId()) isEqualTo restoredNests[1]
    }

    @Test
    fun `should return empty optional for empty nest slot`() {
        expectThat(nestingGroundService.atNestSlot(Slot.S1).isPresent).isFalse()
    }

    @Test
    fun `should return default nest if none is set`() {
        expectThat(nestingGroundService.currentNest().slot) isEqualTo Slot.DEFAULT
    }

    @Test
    fun `should update and return current nest`() {
        // when
        nestingGroundService.moveToNestAt(Slot.S2)

        // then
        expectThat(nestingGroundService.currentNest().slot) isEqualTo Slot.S2
        expectThat(nestingGroundService.currentNestSlot()) isEqualTo Slot.S2
    }

    @Test
    fun `should not update anything if nest is not deployed`() {
        // when
        nestingGroundService.moveToNestAt(Slot.S1)

        // then
        expectThat(nestingGroundService.currentNest().slot) isNotEqualTo Slot.S1
    }

    @Test
    fun `should deploy nest`() {
        // given
        val nestShell = shellOf("Nest")

        // when
        nestingGroundService.place(nestShell, Slot.S5)
        val actual = nestingGroundService.atNestSlot(Slot.S5)

        // then
        expectThat(actual.isPresent).isTrue()
        expectThat(actual.get().viewNestId()) isEqualTo nestShell
        expectThat(actual.get().slot) isEqualTo Slot.S5
    }

    @Test
    fun `should expose explicit nests as snapshot`() {
        expectThat(nestingGroundService.snapshot()) isEqualTo restoredNests
    }
}

private fun NestingGroundService.populateAndList(nestShells: List<de.pflugradts.passbird.domain.model.shell.Shell>) = apply {
    populate(nestShells)
}.all().toList()
