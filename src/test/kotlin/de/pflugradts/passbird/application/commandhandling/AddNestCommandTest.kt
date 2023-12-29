package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.nest.AddNestCommandHandler
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.nestSlotAt
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.nest.createNestServiceForTesting
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue

@Tag(INTEGRATION)
class AddNestCommandTest {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val nestService = createNestServiceForTesting()
    private val addNestCommandHandler = AddNestCommandHandler(nestService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(addNestCommandHandler)

    @Test
    fun `should handle add nest command`() {
        // given
        val nestSlotIndex = 1
        val givenInput = shellOf("n+$nestSlotIndex")
        val nestSlotFromInput = nestSlotAt(nestSlotIndex)
        val referenceNest = shellOf("mynest")
        val givenNest = shellOf("mynest")
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(givenNest)))

        // when
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
        expectThat(nestService.nestShellAtSlot(nestSlotFromInput)) isEqualTo referenceNest
        expectThat(givenNest) isNotEqualTo referenceNest
    }

    @Test
    fun `should update existing nest`() {
        // given
        val nestSlotIndex = 1
        val input = inputOf(shellOf("n+$nestSlotIndex"))
        val nestSlotFromInput = nestSlotAt(nestSlotIndex)
        val referenceNest = shellOf("mynest")
        val givenNest = shellOf("mynest")
        val otherNest = shellOf("othernest")
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(givenNest)))
        nestService.place(otherNest, nestSlotFromInput)
        expectThat(nestService.nestShellAtSlot(nestSlotFromInput)) isEqualTo otherNest

        // when
        inputHandler.handleInput(input)

        // then
        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
        expectThat(nestService.nestShellAtSlot(nestSlotFromInput)) isEqualTo referenceNest
        expectThat(givenNest) isNotEqualTo referenceNest
    }

    @Test
    fun `should handle empty input`() {
        // given
        val nestSlotIndex = 1
        val input = inputOf(shellOf("n+$nestSlotIndex"))
        val nestSlotFromInput = nestSlotAt(nestSlotIndex)
        val givenNest = shellOf("")
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(givenNest)))
        val outputSlot = slot<Output>()

        // when
        inputHandler.handleInput(input)

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell.asString()) contains "Empty input"
        expectThat(nestService.atNestSlot(nestSlotFromInput).isEmpty).isTrue()
    }

    @Test
    fun `should handle add nest command for default nestSlot`() {
        // given
        val nestSlotIndex = 0
        val input = inputOf(shellOf("n+$nestSlotIndex"))
        val outputSlot = slot<Output>()

        // when
        inputHandler.handleInput(input)

        // then
        verify(exactly = 0) { userInterfaceAdapterPort.receive(any()) }
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell.asString()) contains "Default Nest cannot be replaced"
    }

    private fun NestService.nestShellAtSlot(nestSlot: NestSlot) = atNestSlot(nestSlot).orNull()?.viewNestId() ?: emptyShell()
}
