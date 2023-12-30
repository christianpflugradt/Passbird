package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.adapter.userinterface.CommandLineInterfaceService
import de.pflugradts.passbird.application.commandhandling.handler.nest.ViewNestCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.nestSlotAt
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.service.nest.createNestServiceForTesting
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains

@Tag(INTEGRATION)
class ViewNestCommandTest {

    private val configuration = mockk<Configuration>()
    private val commandLineInterfaceService = CommandLineInterfaceService(mockk(), configuration)
    private val nestService = createNestServiceForTesting()
    private val viewNestCommandHandler = ViewNestCommandHandler(nestService, commandLineInterfaceService)
    private val inputHandler = createInputHandlerFor(viewNestCommandHandler)

    @Test
    fun `should print info`() {
        // given
        val input = inputOf(shellOf("n"))
        fakeConfiguration(instance = configuration)
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        captureSystemOut.during {
            inputHandler.handleInput(input)
        }

        // then
        expectThat(captureSystemOut.capture) contains "Available Nest commands"
    }

    @Test
    fun `should print default nest if current`() {
        // given
        val input = inputOf(shellOf("n"))
        fakeConfiguration(instance = configuration)
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        captureSystemOut.during {
            inputHandler.handleInput(input)
        }

        // then
        expectThat(captureSystemOut.capture) contains "Current Nest:"
        expectThat(captureSystemOut.capture) contains "Default"
    }

    @Test
    fun `should print deployed nest`() {
        // given
        val input = inputOf(shellOf("n"))
        val deployedNestSlot = 3
        val deployedNest = "mynest"
        nestService.place(shellOf(deployedNest), nestSlotAt(deployedNestSlot))
        fakeConfiguration(instance = configuration)
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        captureSystemOut.during {
            inputHandler.handleInput(input)
        }

        // then
        expectThat(captureSystemOut.capture) contains "$deployedNestSlot: $deployedNest"
    }
}
