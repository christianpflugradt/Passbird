package de.pflugradts.passbird.application.commandhandling.favorite

import de.pflugradts.kotlinextensions.TryResult.Companion.failure
import de.pflugradts.kotlinextensions.TryResult.Companion.success
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.createInputHandlerFor
import de.pflugradts.passbird.application.commandhandling.handler.favorite.GetFavoriteCommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.S1
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
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

@Tag(INTEGRATION)
class GetFavoriteCommandTest {
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val clipboardAdapterPort = mockk<ClipboardAdapterPort>(relaxed = true)
    private val passwordService = mockk<PasswordService>()
    private val getFavoriteCommandHandler = GetFavoriteCommandHandler(passwordService, clipboardAdapterPort, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(getFavoriteCommandHandler)

    @BeforeEach
    fun setup() {
        every { clipboardAdapterPort.post(any()) } returns success(Unit)
    }

    @Test
    fun `should handle get favorite command`() {
        fakePasswordService(instance = passwordService, withFavorites = mapOf(S1 to "eggid1"))
        val outputSlot = slot<Output>()

        inputHandler.handleInput(inputOf(shellOf("f1")))

        verify { clipboardAdapterPort.post(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell) isEqualTo shellOf("eggid1")
        verify(exactly = 1) { userInterfaceAdapterPort.send(outputOf(shellOf("EggId copied to clipboard."))) }
    }

    @Test
    fun `should handle get favorite command on empty favorite slot`() {
        fakePasswordService(instance = passwordService, withFavorites = emptyMap())

        inputHandler.handleInput(inputOf(shellOf("f1")))

        verify { clipboardAdapterPort wasNot Called }
        verify(exactly = 1) { userInterfaceAdapterPort.send(outputOf(shellOf("Favorite entry at slot 1 does not exist."))) }
    }

    @Test
    fun `should not report successful clipboard copy when favorite clipboard update fails`() {
        fakePasswordService(instance = passwordService, withFavorites = mapOf(S1 to "eggid1"))
        every { clipboardAdapterPort.post(any()) } returns failure(IllegalStateException("clipboard unavailable"))
        val outputSlot = slot<Output>()

        inputHandler.handleInput(inputOf(shellOf("f1")))

        verify { clipboardAdapterPort.post(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell) isEqualTo shellOf("eggid1")
        verify(exactly = 0) { userInterfaceAdapterPort.send(outputOf(shellOf("EggId copied to clipboard."))) }
    }
}
