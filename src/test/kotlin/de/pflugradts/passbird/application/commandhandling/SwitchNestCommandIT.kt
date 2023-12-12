package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.nest.SwitchNestCommandHandler
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.createNestServiceForTesting
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

class SwitchNestCommandIT {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val nestService = createNestServiceForTesting()
    private val switchNestCommandHandler = SwitchNestCommandHandler(nestService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(switchNestCommandHandler)

    @Test
    fun `should handle switch nest command`() {
        // given
        val givenNestSlot = Slot.N1
        nestService.deploy(shellOf("nest"), givenNestSlot)
        val input = inputOf(shellOf("n" + givenNestSlot.index()))

        // when
        inputHandler.handleInput(input)

        // then
        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
        expectThat(nestService.getCurrentNest().slot) isEqualTo givenNestSlot
    }

    @Test
    fun `should do nothing if nest is already current`() {
        // given
        val givenNestSlot = Slot.N1
        nestService.deploy(shellOf("nest"), givenNestSlot)
        nestService.moveToNestAt(givenNestSlot)
        val input = inputOf(shellOf("n" + givenNestSlot.index()))
        val outputSlot = slot<Output>()

        // when
        inputHandler.handleInput(input)

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell.asString()) contains "is already the current namespace"
    }

    @Test
    fun `should do nothing if nest is not deployed`() {
        // given
        val givenNestSlot = Slot.N1
        val input = inputOf(shellOf("n" + givenNestSlot.index()))
        expectThat(nestService.atSlot(givenNestSlot).isEmpty).isTrue()
        val outputSlot = slot<Output>()

        // when
        inputHandler.handleInput(input)

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell.asString()) contains "Specified namespace does not exist"
    }
}
