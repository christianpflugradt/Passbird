package de.pflugradts.passbird.application.commandhandling.yolk

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.adapter.userinterface.CommandLineInterfaceService
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.createInputHandlerFor
import de.pflugradts.passbird.application.commandhandling.handler.yolk.YolkInfoCommandHandler
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
class YolkInfoCommandTest {

    private val configuration = mockk<Configuration>()
    private val commandLineInterfaceService = CommandLineInterfaceService(mockk(), configuration)
    private val yolkInfoCommandHandler = YolkInfoCommandHandler(CanPrintInfo(), commandLineInterfaceService)
    private val inputHandler = createInputHandlerFor(yolkInfoCommandHandler)

    @Test
    fun `should print info`() {
        // given
        val input = inputOf(shellOf("y?"))
        fakeConfiguration(instance = configuration)
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        captureSystemOut.during {
            inputHandler.handleInput(input)
        }

        // then
        expectThat(captureSystemOut.capture) contains "Available Yolk commands"
        expectThat(captureSystemOut.capture) contains
            "  y+[EggId]          (set)        Prompts for a TOTP secret or otpauth URI and stores it for the specified Egg."
        expectThat(captureSystemOut.capture).not().contains("\t")
    }
}
