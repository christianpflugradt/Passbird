package de.pflugradts.passbird.application.commandhandling.favorite

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.adapter.userinterface.CommandLineInterfaceService
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.createInputHandlerFor
import de.pflugradts.passbird.application.commandhandling.handler.favorite.ViewFavoriteCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot.S1
import de.pflugradts.passbird.domain.model.slot.Slot.S3
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isFalse

@Tag(INTEGRATION)
class ViewFavoriteCommandTest {
    private val configuration = mockk<Configuration>()
    private val commandLineInterfaceService = CommandLineInterfaceService(mockk(), configuration)
    private val passwordService = mockk<PasswordService>()
    private val viewFavoriteCommandHandler = ViewFavoriteCommandHandler(CanPrintInfo(), passwordService, commandLineInterfaceService)
    private val inputHandler = createInputHandlerFor(viewFavoriteCommandHandler)

    @Test
    fun `should handle view favorite command`() {
        fakeConfiguration(instance = configuration)
        fakePasswordService(
            instance = passwordService,
            withFavorites = mapOf(
                S1 to "eggid1",
                S3 to "eggid3",
            ),
        )
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        captureSystemOut.during {
            inputHandler.handleInput(inputOf(shellOf("f")))
        }

        expectThat(captureSystemOut.capture) contains "1: eggid1\n"
        expectThat(captureSystemOut.capture) contains "3: eggid3\n"
        expectThat(captureSystemOut.capture.contains("0:")).isFalse()
    }

    @Test
    fun `should handle view favorite command on empty favorites`() {
        fakeConfiguration(instance = configuration)
        fakePasswordService(instance = passwordService, withFavorites = emptyMap())
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        captureSystemOut.during {
            inputHandler.handleInput(inputOf(shellOf("f")))
        }

        expectThat(captureSystemOut.capture) contains "Favorite EggIds are empty."
    }
}
