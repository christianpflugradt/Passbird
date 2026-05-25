package de.pflugradts.passbird.application.commandhandling.favorite

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.adapter.userinterface.CommandLineInterfaceService
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.createInputHandlerFor
import de.pflugradts.passbird.application.commandhandling.handler.favorite.FavoriteInfoCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains

@Tag(INTEGRATION)
class FavoriteInfoCommandTest {
    private val configuration = mockk<Configuration>()
    private val commandLineInterfaceService = CommandLineInterfaceService(mockk(), configuration)
    private val favoriteInfoCommandHandler = FavoriteInfoCommandHandler(CanPrintInfo(), commandLineInterfaceService)
    private val inputHandler = createInputHandlerFor(favoriteInfoCommandHandler)

    @Test
    fun `should print info`() {
        val input = inputOf(shellOf("f?"))
        fakeConfiguration(instance = configuration)
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        captureSystemOut.during {
            inputHandler.handleInput(input)
        }

        expectThat(captureSystemOut.capture) contains "Available Favorite commands"
    }
}
