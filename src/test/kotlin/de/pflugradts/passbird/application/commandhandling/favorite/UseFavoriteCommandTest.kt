package de.pflugradts.passbird.application.commandhandling.favorite

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.InputHandler
import de.pflugradts.passbird.application.commandhandling.createInputHandlerFor
import de.pflugradts.passbird.application.commandhandling.handler.favorite.UseFavoriteCommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot.S1
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag(INTEGRATION)
class UseFavoriteCommandTest {
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val passwordService = mockk<PasswordService>()
    private val mockedInputHandler = mockk<InputHandler>()
    private val commandExecutionTracker = CommandExecutionTracker()
    private val useFavoriteCommandHandler =
        UseFavoriteCommandHandler(mockedInputHandler, passwordService, userInterfaceAdapterPort, commandExecutionTracker)
    private val inputHandler = createInputHandlerFor(useFavoriteCommandHandler, commandExecutionTracker)

    @Test
    fun `should handle use favorite command`() {
        val favoritedEggId = "eggId"
        val forwardCommand = "p0"
        fakePasswordService(instance = passwordService, withFavorites = mapOf(S1 to favoritedEggId))

        inputHandler.handleInput(inputOf(shellOf("f1$forwardCommand")))

        verify(exactly = 1) { mockedInputHandler.handleInput(inputOf(shellOf("$forwardCommand$favoritedEggId"))) }
    }

    @Test
    fun `should handle use favorite command on empty favorite slot`() {
        fakePasswordService(instance = passwordService, withFavorites = emptyMap())

        inputHandler.handleInput(inputOf(shellOf("f1p0")))

        verify { mockedInputHandler wasNot Called }
        verify(exactly = 1) { userInterfaceAdapterPort.send(outputOf(shellOf("Favorite entry at slot 1 does not exist."))) }
    }
}
