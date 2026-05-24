package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream
import de.pflugradts.kotlinextensions.CapturedOutputPrintStream.Companion.captureSystemErr
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.adapter.userinterface.CommandLineInterfaceService
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.handler.HelpCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo

@Tag(INTEGRATION)
class HelpCommandTest {

    private val configuration = mockk<Configuration>()
    private val commandLineInterfaceService = CommandLineInterfaceService(mockk(), configuration)
    private val helpCommandHandler = HelpCommandHandler(CanPrintInfo(), commandLineInterfaceService)
    private val inputHandler = createInputHandlerFor(helpCommandHandler)

    @Test
    fun `should handle help command`() {
        // given
        val input = Input.inputOf(shellOf("h"))
        fakeConfiguration(instance = configuration)
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        captureSystemOut.during {
            inputHandler.handleInput(input)
        }

        // then
        expectThat(captureSystemOut.capture) contains "Usage: [command][parameter]"
        expectThat(captureSystemOut.capture) contains "k (keystore)"
        expectThat(captureSystemOut.capture) contains "Lists all EggIds across all Nests, grouped by Nest."
    }

    @Test
    fun `should reject help command with trailing input`() {
        // given
        val input = Input.inputOf(shellOf("hhelp"))
        fakeConfiguration(instance = configuration)
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()
        val captureSystemErr = captureSystemErr()

        // when
        captureSystemErr.during {
            captureSystemOut.during {
                inputHandler.handleInput(input)
            }
        }

        // then
        expectThat(captureSystemOut.capture) isEqualTo ""
        expectThat(captureSystemErr.capture) isEqualTo "Command execution failed: Parameter for command 'h' not supported: help\n"
    }
}
