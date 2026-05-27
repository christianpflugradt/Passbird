package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream.Companion.captureSystemErr
import de.pflugradts.passbird.application.InactivityTerminationRequestedException
import de.pflugradts.passbird.application.StdinTerminationRequestedException
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class CommandInputHandlerTest {
    @Test
    fun `should rethrow inactivity termination without reporting a command failure`() {
        // given
        val commandBus = mockk<CommandBus>()
        val inputHandler = createInputHandlerFor(commandBus)
        val captureSystemErr = captureSystemErr()
        every { commandBus.post(any()) } throws InactivityTerminationRequestedException()

        // when
        captureSystemErr.during {
            assertThrows<InactivityTerminationRequestedException> {
                inputHandler.handleInput(inputOf(shellOf("q")))
            }
        }

        // then
        expectThat(captureSystemErr.capture) isEqualTo ""
    }

    @Test
    fun `should rethrow stdin termination without reporting a command failure`() {
        // given
        val commandBus = mockk<CommandBus>()
        val inputHandler = createInputHandlerFor(commandBus)
        val captureSystemErr = captureSystemErr()
        every { commandBus.post(any()) } throws StdinTerminationRequestedException()

        // when
        captureSystemErr.during {
            assertThrows<StdinTerminationRequestedException> {
                inputHandler.handleInput(inputOf(shellOf("q")))
            }
        }

        // then
        expectThat(captureSystemErr.capture) isEqualTo ""
    }
}
