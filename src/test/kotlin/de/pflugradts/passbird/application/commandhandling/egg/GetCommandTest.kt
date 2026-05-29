package de.pflugradts.passbird.application.commandhandling.egg

import de.pflugradts.kotlinextensions.TryResult.Companion.failure
import de.pflugradts.kotlinextensions.TryResult.Companion.success
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.createInputHandlerFor
import de.pflugradts.passbird.application.commandhandling.handler.egg.GetCommandHandler
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

@Tag(INTEGRATION)
class GetCommandTest {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val clipboardAdapterPort = mockk<ClipboardAdapterPort>(relaxed = true)
    private val passwordService = mockk<PasswordService>()
    private val commandExecutionTracker = CommandExecutionTracker()
    private val getCommandHandler =
        GetCommandHandler(passwordService, clipboardAdapterPort, userInterfaceAdapterPort, commandExecutionTracker)
    private val inputHandler = createInputHandlerFor(getCommandHandler, commandExecutionTracker)

    @BeforeEach
    fun setup() {
        every { clipboardAdapterPort.post(any()) } returns success(Unit)
    }

    @Test
    fun `should handle get command`() {
        // given
        val args = "EggId"
        val command = shellOf("g$args")
        val reference = command.copy()
        val expectedPassword = shellOf("value")
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(createEggForTesting(withEggIdShell = shellOf(args), withPasswordShell = expectedPassword)),
        )
        val outputSlot = slot<Output>()
        every { clipboardAdapterPort.post(capture(outputSlot)) } answers {
            expectThat(outputSlot.captured.shell) isEqualTo expectedPassword
            success(Unit)
        }

        // when
        expectThat(command) isEqualTo reference
        inputHandler.handleInput(inputOf(command))

        // then
        verify { clipboardAdapterPort.post(any()) }
        expectThat(outputSlot.captured.shell) isNotEqualTo expectedPassword
        verify(exactly = 1) { userInterfaceAdapterPort.send(any()) }
        expectThat(command) isNotEqualTo reference
    }

    @Test
    fun `should handle get command with invalid egg`() {
        // given
        val args = "EggId"
        val command = shellOf("g$args")
        val reference = command.copy()
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(createEggForTesting(withEggIdShell = shellOf("other"))),
        )

        // when
        expectThat(command) isEqualTo reference
        inputHandler.handleInput(inputOf(command))

        // then
        verify { clipboardAdapterPort wasNot Called }
        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
        expectThat(command) isNotEqualTo reference
    }

    @Test
    fun `should not report successful clipboard copy when clipboard update fails`() {
        // given
        val args = "EggId"
        val command = shellOf("g$args")
        val reference = command.copy()
        val expectedPassword = shellOf("value")
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(createEggForTesting(withEggIdShell = shellOf(args), withPasswordShell = expectedPassword)),
        )
        every { clipboardAdapterPort.post(any()) } returns failure(IllegalStateException("clipboard unavailable"))
        val outputSlot = slot<Output>()
        every { clipboardAdapterPort.post(capture(outputSlot)) } answers {
            expectThat(outputSlot.captured.shell) isEqualTo expectedPassword
            failure(IllegalStateException("clipboard unavailable"))
        }

        // when
        expectThat(command) isEqualTo reference
        inputHandler.handleInput(inputOf(command))

        // then
        verify { clipboardAdapterPort.post(any()) }
        expectThat(outputSlot.captured.shell) isNotEqualTo expectedPassword
        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
        expectThat(command) isNotEqualTo reference
    }
}
