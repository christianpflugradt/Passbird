package de.pflugradts.passbird.application.commandhandling.memory

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.adapter.userinterface.CommandLineInterfaceService
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.createInputHandlerFor
import de.pflugradts.passbird.application.commandhandling.handler.memory.ViewMemoryCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.slot.Slot.S1
import de.pflugradts.passbird.domain.model.slot.Slot.S2
import de.pflugradts.passbird.domain.model.slot.Slot.S3
import de.pflugradts.passbird.domain.model.slot.Slot.S4
import de.pflugradts.passbird.domain.model.slot.Slot.S5
import de.pflugradts.passbird.domain.model.slot.Slot.S6
import de.pflugradts.passbird.domain.model.slot.Slot.S7
import de.pflugradts.passbird.domain.model.slot.Slot.S8
import de.pflugradts.passbird.domain.model.slot.Slot.S9
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains

@Tag(INTEGRATION)
class ViewMemoryCommandTest {

    private val configuration = mockk<Configuration>()
    private val commandLineInterfaceService = CommandLineInterfaceService(mockk(), configuration)
    private val passwordService = mockk<PasswordService>()
    private val viewMemoryCommandHandler = ViewMemoryCommandHandler(CanPrintInfo(), passwordService, commandLineInterfaceService)
    private val inputHandler = createInputHandlerFor(viewMemoryCommandHandler)

    @Test
    fun `should handle view memory command`() {
        // given
        val command = shellOf("m")
        fakeConfiguration(instance = configuration)
        fakePasswordService(
            instance = passwordService,
            withMemory = mapOf(
                DEFAULT to "eggid0",
                S1 to "eggid1",
                S2 to "eggid2",
                S3 to "eggid3",
                S4 to "eggid4",
                S5 to "eggid5",
                S6 to "eggid6",
                S7 to "eggid7",
                S8 to "eggid8",
                S9 to "eggid9",
            ),
        )
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        captureSystemOut.during {
            inputHandler.handleInput(inputOf(command))
        }

        // then
        for (i in 0 until 10) {
            expectThat(captureSystemOut.capture) contains "$i: eggid$i\n"
        }
    }

    @Test
    fun `should handle view memory command on empty memory`() {
        // given
        val command = shellOf("m")
        fakeConfiguration(instance = configuration)
        fakePasswordService(instance = passwordService, withMemory = emptyMap())
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        captureSystemOut.during {
            inputHandler.handleInput(inputOf(command))
        }

        // then
        expectThat(captureSystemOut.capture) contains "EggIdMemory is empty."
    }
}
