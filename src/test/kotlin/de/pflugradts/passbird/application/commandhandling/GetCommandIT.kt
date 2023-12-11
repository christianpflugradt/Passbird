package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.GetCommandHandler
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.Called
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

class GetCommandIT {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val clipboardAdapterPort = mockk<ClipboardAdapterPort>(relaxed = true)
    private val passwordService = mockk<PasswordService>()
    private val getCommandHandler = GetCommandHandler(passwordService, clipboardAdapterPort, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(getCommandHandler)

    @Test
    fun `should handle get command`() {
        // given
        val args = "key"
        val command = bytesOf("g$args")
        val reference = command.copy()
        val expectedPassword = bytesOf("value")
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(createEggForTesting(withKeyBytes = bytesOf(args), withPasswordBytes = expectedPassword)),
        )
        val outputSlot = slot<Output>()

        // when
        expectThat(command) isEqualTo reference
        inputHandler.handleInput(inputOf(command))

        // then
        verify { clipboardAdapterPort.post(capture(outputSlot)) }
        expectThat(outputSlot.captured.bytes) isEqualTo expectedPassword
        verify(exactly = 1) { userInterfaceAdapterPort.send(any()) }
        expectThat(command) isNotEqualTo reference
    }

    @Test
    fun `should handle get command with invalid egg`() {
        // given
        val args = "key"
        val command = bytesOf("g$args")
        val reference = command.copy()
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(createEggForTesting(withKeyBytes = bytesOf("other"))),
        )

        // when
        expectThat(command) isEqualTo reference
        inputHandler.handleInput(inputOf(command))

        // then
        verify { clipboardAdapterPort wasNot Called }
        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
        expectThat(command) isNotEqualTo reference
    }
}
