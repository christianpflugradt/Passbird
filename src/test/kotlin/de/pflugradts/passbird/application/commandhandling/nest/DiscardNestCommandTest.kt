package de.pflugradts.passbird.application.commandhandling.nest

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.createInputHandlerFor
import de.pflugradts.passbird.application.commandhandling.handler.nest.DiscardNestCommandHandler
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.slotAt
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.slot.Slot.S5
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.nest.createNestServiceForTesting
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@Tag(INTEGRATION)
class DiscardNestCommandTest {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val nestService = createNestServiceForTesting()
    private val passwordService = mockk<PasswordService>()
    private val discardNestCommandHandler = DiscardNestCommandHandler(nestService, passwordService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(discardNestCommandHandler)

    @Test
    fun `should handle discard nest command`() {
        // given
        val nestSlotIndex = 1
        val givenInput = shellOf("n-$nestSlotIndex")
        val nestSlotFromInput = slotAt(nestSlotIndex)
        val givenNest = shellOf("mynest")
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort)
        fakePasswordService(instance = passwordService, withEggs = emptyList())
        nestService.place(givenNest, nestSlotFromInput)

        // when
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
        expectThat(nestService.atNestSlot(nestSlotFromInput).isEmpty).isTrue()
    }

    @Test
    fun `should abort discard nest if specified nest is default nest`() {
        // given
        val nestSlotIndex = 0
        val givenInput = shellOf("n-$nestSlotIndex")
        val nestSlotFromInput = slotAt(nestSlotIndex)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort)
        fakePasswordService(instance = passwordService, withEggs = emptyList())
        val outputSlot = slot<Output>()

        // when
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell.asString()) isEqualTo "Default Nest cannot be discarded - Operation aborted."
        expectThat(nestService.atNestSlot(nestSlotFromInput).isEmpty).isFalse()
    }

    @Test
    fun `should abort discard nest if specified nest does not exist`() {
        // given
        val nestSlotIndex = 1
        val givenInput = shellOf("n-$nestSlotIndex")
        val nestSlotFromInput = slotAt(nestSlotIndex)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort)
        fakePasswordService(instance = passwordService, withEggs = emptyList())
        val outputSlot = slot<Output>()

        // when
        expectThat(nestService.atNestSlot(nestSlotFromInput).isEmpty).isTrue()
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell.asString()) isEqualTo "Specified Nest does not exist - Operation aborted."
    }

    @Test
    fun `should remain in current nest`() {
        // given
        val nestSlotIndex = 1
        val givenInput = shellOf("n-$nestSlotIndex")
        val nestSlotFromInput = slotAt(nestSlotIndex)
        val givenNest = shellOf("mynest")
        val currentNestSlot = S5
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort)
        fakePasswordService(instance = passwordService, withEggs = emptyList())
        nestService.place(givenNest, nestSlotFromInput)
        nestService.place(shellOf("current"), currentNestSlot)
        nestService.moveToNestAt(currentNestSlot)

        // when
        inputHandler.handleInput(inputOf(givenInput))

        // then
        expectThat(nestService.atNestSlot(nestSlotFromInput).isEmpty).isTrue()
        expectThat(nestService.currentNest().slot) isEqualTo currentNestSlot
    }

    @Test
    fun `should move to default nest if current nest is discarded`() {
        // given
        val nestSlotIndex = 1
        val givenInput = shellOf("n-$nestSlotIndex")
        val nestSlotFromInput = slotAt(nestSlotIndex)
        val givenNest = shellOf("mynest")
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort)
        fakePasswordService(instance = passwordService, withEggs = emptyList())
        nestService.place(givenNest, nestSlotFromInput)
        nestService.moveToNestAt(nestSlotFromInput)

        // when
        inputHandler.handleInput(inputOf(givenInput))

        // then
        expectThat(nestService.atNestSlot(nestSlotFromInput).isEmpty).isTrue()
        expectThat(nestService.currentNest().slot) isEqualTo DEFAULT
    }

    @Test
    fun `should move eggs to specified nest if discarded nest is not empty`() {
        // given
        val nestSlotIndex = 1
        val givenInput = shellOf("n-$nestSlotIndex")
        val nestSlotFromInput = slotAt(nestSlotIndex)
        val givenNest = shellOf("discardnest")
        val targetNest = shellOf("keepnest")
        val targetNestSlot = S5
        val eggId1 = shellOf("EggId1")
        val eggId2 = shellOf("EggId2")
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf("${targetNestSlot.index()}"))),
        )
        fakePasswordService(
            instance = passwordService,
            withNestService = nestService,
            withEggs = listOf(
                createEggForTesting(withEggIdShell = eggId1, withSlot = nestSlotFromInput),
                createEggForTesting(withEggIdShell = eggId2, withSlot = nestSlotFromInput),
            ),
        )
        nestService.place(givenNest, nestSlotFromInput)
        nestService.place(targetNest, targetNestSlot)

        // when
        inputHandler.handleInput(inputOf(givenInput))
        nestService.moveToNestAt(targetNestSlot)

        // then
        verify(exactly = 1) { passwordService.moveEgg(eggId1, targetNestSlot) }
        verify(exactly = 1) { passwordService.moveEgg(eggId2, targetNestSlot) }
        expectThat(nestService.atNestSlot(nestSlotFromInput).isEmpty).isTrue()
    }

    @Test
    fun `should abort if target nest for eggs to move does not exist`() {
        // given
        val nestSlotIndex = 1
        val givenInput = shellOf("n-$nestSlotIndex")
        val nestSlotFromInput = slotAt(nestSlotIndex)
        val givenNest = shellOf("discardnest")
        val nonexistentNestSlot = S5
        val eggId1 = shellOf("EggId1")
        val eggId2 = shellOf("EggId2")
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf("${nonexistentNestSlot.index()}"))),
        )
        fakePasswordService(
            instance = passwordService,
            withNestService = nestService,
            withEggs = listOf(
                createEggForTesting(withEggIdShell = eggId1, withSlot = nestSlotFromInput),
                createEggForTesting(withEggIdShell = eggId2, withSlot = nestSlotFromInput),
            ),
        )
        nestService.place(givenNest, nestSlotFromInput)
        val outputSlot = slot<Output>()

        // when
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify(exactly = 0) { passwordService.moveEgg(eggId1, nonexistentNestSlot) }
        verify(exactly = 0) { passwordService.moveEgg(eggId2, nonexistentNestSlot) }
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell.asString()) isEqualTo "Nest Slot ${nonexistentNestSlot.index()} is empty - Operation aborted."
        expectThat(nestService.atNestSlot(nestSlotFromInput).isEmpty).isFalse()
    }

    @Test
    fun `should abort if target nest contains eggs present in nest to be discarded`() {
        // given
        val nestSlotIndex = 1
        val givenInput = shellOf("n-$nestSlotIndex")
        val nestSlotFromInput = slotAt(nestSlotIndex)
        val givenNest = shellOf("discardnest")
        val targetNest = shellOf("keepnest")
        val targetNestSlot = S5
        val eggId1 = shellOf("EggId1")
        val eggId2 = shellOf("EggId2")
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf("${targetNestSlot.index()}"))),
        )
        fakePasswordService(
            instance = passwordService,
            withNestService = nestService,
            withEggs = listOf(
                createEggForTesting(withEggIdShell = eggId1, withSlot = nestSlotFromInput),
                createEggForTesting(withEggIdShell = eggId2, withSlot = nestSlotFromInput),
                createEggForTesting(withEggIdShell = eggId1, withSlot = targetNestSlot),
                createEggForTesting(withEggIdShell = eggId2, withSlot = targetNestSlot),
            ),
        )
        nestService.place(givenNest, nestSlotFromInput)
        nestService.place(targetNest, targetNestSlot)
        val outputSlot = mutableListOf<Output>()

        // when
        inputHandler.handleInput(inputOf(givenInput))
        nestService.moveToNestAt(targetNestSlot)

        // then
        verify(exactly = 0) { passwordService.moveEgg(eggId1, targetNestSlot) }
        verify(exactly = 0) { passwordService.moveEgg(eggId2, targetNestSlot) }
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot) hasSize 2
        expectThat(outputSlot[0].shell.asString()) isEqualTo "The following EggIds exist in both Nests. " +
            "Please move them manually before discarding the Nest: \n- EggId1\n- EggId2"
        expectThat(outputSlot[1].shell.asString()) isEqualTo "Operation aborted."
        expectThat(nestService.atNestSlot(nestSlotFromInput).isEmpty).isFalse()
    }
}
