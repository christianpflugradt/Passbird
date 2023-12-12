package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.nest.AddNestCommandHandler
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.nest.Slot
import de.pflugradts.passbird.domain.model.nest.Slot.Companion.at
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.NestService
import de.pflugradts.passbird.domain.service.createNestServiceForTesting
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue
import kotlin.jvm.optionals.getOrNull

class AddNestCommandIT {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val nestService = createNestServiceForTesting()
    private val addNestCommandHandler = AddNestCommandHandler(nestService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(addNestCommandHandler)

    @Test
    fun `should handle add nest command`() {
        // given
        val slotIndex = 1
        val givenInput = shellOf("n+$slotIndex")
        val slotFromInput = at(slotIndex)
        val referenceNest = shellOf("mynest")
        val givenNest = shellOf("mynest")
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(givenNest)))

        // when
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
        expectThat(nestService.nestShellAtSlot(slotFromInput)) isEqualTo referenceNest
        expectThat(givenNest) isNotEqualTo referenceNest
    }

    @Test
    fun `should update existing nest`() {
        // given
        val slotIndex = 1
        val input = inputOf(shellOf("n+$slotIndex"))
        val slotFromInput = at(slotIndex)
        val referenceNest = shellOf("mynest")
        val givenNest = shellOf("mynest")
        val otherNest = shellOf("othernest")
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(givenNest)))
        nestService.deploy(otherNest, slotFromInput)
        expectThat(nestService.nestShellAtSlot(slotFromInput)) isEqualTo otherNest

        // when
        inputHandler.handleInput(input)

        // then
        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
        expectThat(nestService.nestShellAtSlot(slotFromInput)) isEqualTo referenceNest
        expectThat(givenNest) isNotEqualTo referenceNest
    }

    @Test
    fun `should handle empty input`() {
        // given
        val slotIndex = 1
        val input = inputOf(shellOf("n+$slotIndex"))
        val slotFromInput = at(slotIndex)
        val givenNest = shellOf("")
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(givenNest)))
        val outputSlot = slot<Output>()

        // when
        inputHandler.handleInput(input)

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell.asString()) contains "Empty input"
        expectThat(nestService.atSlot(slotFromInput).isEmpty).isTrue()
    }

    @Test
    fun `should handle add nest command for default slot`() {
        // given
        val slotIndex = 0
        val input = inputOf(shellOf("n+$slotIndex"))
        val outputSlot = slot<Output>()

        // when
        inputHandler.handleInput(input)

        // then
        verify(exactly = 0) { userInterfaceAdapterPort.receive(any()) }
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell.asString()) contains "Default namespace cannot be replaced"
    }

    private fun NestService.nestShellAtSlot(slot: Slot) = atSlot(slot).getOrNull()?.shell ?: emptyShell()
}
