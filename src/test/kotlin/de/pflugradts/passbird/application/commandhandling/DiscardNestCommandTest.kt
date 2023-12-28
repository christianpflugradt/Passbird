package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.nest.DiscardNestCommandHandler
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.nestSlotAt
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.createNestServiceForTesting
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
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
        val givenInput = Shell.shellOf("n-$nestSlotIndex")
        val nestSlotFromInput = nestSlotAt(nestSlotIndex)
        val givenNest = Shell.shellOf("mynest")
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
        val givenInput = Shell.shellOf("n-$nestSlotIndex")
        val nestSlotFromInput = nestSlotAt(nestSlotIndex)
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
        val givenInput = Shell.shellOf("n-$nestSlotIndex")
        val nestSlotFromInput = nestSlotAt(nestSlotIndex)
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
}
