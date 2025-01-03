package de.pflugradts.passbird.application.commandhandling.nest

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.createInputHandlerFor
import de.pflugradts.passbird.application.commandhandling.handler.nest.SwitchNestCommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.nest.createNestServiceForTesting
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

@Tag(INTEGRATION)
class SwitchNestCommandTest {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val nestService = createNestServiceForTesting()
    private val switchNestCommandHandler = SwitchNestCommandHandler(nestService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(switchNestCommandHandler)

    @Test
    fun `should handle switch nest command`() {
        // given
        val givenSlot = Slot.S1
        nestService.place(shellOf("Nest"), givenSlot)
        val input = inputOf(shellOf("n" + givenSlot.index()))

        // when
        inputHandler.handleInput(input)

        // then
        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
        expectThat(nestService.currentNest().slot) isEqualTo givenSlot
    }

    @Test
    fun `should do nothing if nest is already current`() {
        // given
        val givenSlot = Slot.S1
        nestService.place(shellOf("Nest"), givenSlot)
        nestService.moveToNestAt(givenSlot)
        val input = inputOf(shellOf("n" + givenSlot.index()))
        val outputSlot = slot<Output>()

        // when
        inputHandler.handleInput(input)

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell.asString()) contains "is already the current Nest"
    }

    @Test
    fun `should do nothing if nest is not deployed`() {
        // given
        val givenSlot = Slot.S1
        val input = inputOf(shellOf("n" + givenSlot.index()))
        expectThat(nestService.atNestSlot(givenSlot).isEmpty).isTrue()
        val outputSlot = slot<Output>()

        // when
        inputHandler.handleInput(input)

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell.asString()) contains "Specified Nest does not exist"
    }
}
