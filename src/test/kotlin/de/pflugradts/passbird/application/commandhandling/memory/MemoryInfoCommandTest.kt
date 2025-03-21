package de.pflugradts.passbird.application.commandhandling.memory

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.adapter.userinterface.CommandLineInterfaceService
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.createInputHandlerFor
import de.pflugradts.passbird.application.commandhandling.handler.memory.MemoryInfoCommandHandler
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
class MemoryInfoCommandTest {

    private val configuration = mockk<Configuration>()
    private val commandLineInterfaceService = CommandLineInterfaceService(mockk(), configuration)
    private val memoryInfoCommandHandler = MemoryInfoCommandHandler(CanPrintInfo(), commandLineInterfaceService)
    private val inputHandler = createInputHandlerFor(memoryInfoCommandHandler)

    @Test
    fun `should print info`() {
        // given
        val input = inputOf(shellOf("m?"))
        fakeConfiguration(instance = configuration)
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        captureSystemOut.during {
            inputHandler.handleInput(input)
        }

        // then
        expectThat(captureSystemOut.capture) contains "Available Memory commands"
    }
}
