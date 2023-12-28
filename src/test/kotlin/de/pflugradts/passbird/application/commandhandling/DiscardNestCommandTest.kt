package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.nest.DiscardNestCommandHandler
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.nestSlotAt
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.service.createNestServiceForTesting
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
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
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(Input.inputOf(givenNest)))
        fakePasswordService(instance = passwordService, withEggs = emptyList())
        nestService.place(givenNest, nestSlotFromInput)

        // when
        inputHandler.handleInput(Input.inputOf(givenInput))

        // then
        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
        expectThat(nestService.atNestSlot(nestSlotFromInput).isEmpty).isTrue()
    }
}
