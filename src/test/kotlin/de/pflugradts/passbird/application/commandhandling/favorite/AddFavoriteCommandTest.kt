package de.pflugradts.passbird.application.commandhandling.favorite

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.createInputHandlerFor
import de.pflugradts.passbird.application.commandhandling.handler.favorite.AddFavoriteCommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot.S1
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag(INTEGRATION)
class AddFavoriteCommandTest {
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val passwordService = mockk<PasswordService>()
    private val commandExecutionTracker = CommandExecutionTracker()
    private val addFavoriteCommandHandler = AddFavoriteCommandHandler(passwordService, userInterfaceAdapterPort, commandExecutionTracker)
    private val inputHandler = createInputHandlerFor(addFavoriteCommandHandler, commandExecutionTracker)

    @Test
    fun `should handle add favorite command`() {
        fakePasswordService(instance = passwordService)

        inputHandler.handleInput(inputOf(shellOf("f+1eggId")))

        verify(exactly = 1) { passwordService.putFavorite(S1, shellOf("eggId")) }
    }
}
