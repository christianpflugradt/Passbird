package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream.Companion.captureSystemOut
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.adapter.userinterface.CommandLineInterfaceService
import de.pflugradts.passbird.application.commandhandling.command.QuitCommand
import de.pflugradts.passbird.application.commandhandling.command.QuitReason.INACTIVITY
import de.pflugradts.passbird.application.commandhandling.handler.QuitCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.fakeSystemOperation
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains

@Tag(INTEGRATION)
class QuitCommandTest {

    private val configuration = mockk<Configuration>()
    private val commandLineInterfaceService = CommandLineInterfaceService(mockk(), configuration)
    private val systemOperation = mockk<SystemOperation>()
    private val quitCommandHandler = QuitCommandHandler(commandLineInterfaceService, systemOperation)

    @BeforeEach
    fun setup() {
        fakeConfiguration(instance = configuration)
        fakeSystemOperation(instance = systemOperation)
    }

    @Nested
    inner class UserTest {
        private val inputHandler = createInputHandlerFor(quitCommandHandler)

        @Test
        fun `should handle quit command`() {
            // given
            val input = inputOf(shellOf("q"))
            val captureSystemOut = captureSystemOut()

            // when
            captureSystemOut.during { inputHandler.handleInput(input) }

            // then
            verify(exactly = 1) { systemOperation.exit() }
            expectThat(captureSystemOut.capture).not().contains("Terminating Passbird due to inactivity")
        }
    }

    @Nested
    inner class InactivityTest {
        private val commandBus = CommandBus(setOf(quitCommandHandler))

        @Test
        fun `should output inactivity hint if quit reason is inactivity`() {
            // given
            val captureSystemOut = captureSystemOut()

            // when
            captureSystemOut.during { commandBus.post(QuitCommand(quitReason = INACTIVITY)) }

            // then
            verify(exactly = 1) { systemOperation.exit() }
            expectThat(captureSystemOut.capture) contains "Terminating Passbird due to inactivity"
        }
    }
}
