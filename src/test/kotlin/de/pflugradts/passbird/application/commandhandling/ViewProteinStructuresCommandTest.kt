package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.adapter.userinterface.CommandLineInterfaceService
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.handler.protein.ViewProteinStructuresCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

@Tag(INTEGRATION)
class ViewProteinStructuresCommandTest {

    private val configuration = mockk<Configuration>()
    private val commandLineInterfaceService = CommandLineInterfaceService(mockk(), configuration)
    private val passwordService = mockk<PasswordService>()
    private val viewProteinStructuresCommandHandler = ViewProteinStructuresCommandHandler(
        CanPrintInfo(),
        passwordService,
        commandLineInterfaceService,
    )
    private val inputHandler = createInputHandlerFor(viewProteinStructuresCommandHandler)

    @BeforeEach
    fun setup() {
        fakeConfiguration(instance = configuration)
    }

    @Test
    fun `should handle view protein structures command for egg without proteins`() {
        // given
        val eggId = "EggId"
        val command = shellOf("p*$eggId")
        val reference = command.copy()
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(createEggForTesting(withEggIdShell = shellOf(eggId))),
        )
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        expectThat(command) isEqualTo reference
        captureSystemOut.during {
            inputHandler.handleInput(inputOf(command))
        }

        // then
        expectThat(captureSystemOut.capture) contains "1:   ---                  | ---                 "
        expectThat(command) isNotEqualTo reference
    }

    @Test
    fun `should handle view protein structures command for egg with protein`() {
        // given
        val eggId = "EggId"
        val command = shellOf("p*$eggId")
        val type = "proteinType"
        val structure = "proteinStructure"
        val reference = command.copy()
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(
                createEggForTesting(
                    withEggIdShell = shellOf(eggId),
                    withProteins = mapOf(Slot.S3 to Pair(shellOf(type), shellOf(structure))),
                ),
            ),
        )
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        expectThat(command) isEqualTo reference
        captureSystemOut.during {
            inputHandler.handleInput(inputOf(command))
        }

        // then
        expectThat(captureSystemOut.capture) contains "3:   proteinType          | proteinStructure "
        expectThat(command) isNotEqualTo reference
    }

    @Test
    fun `should handle view protein structures command for non existing egg`() {
        // given
        val eggId = "EggId"
        val command = shellOf("p*$eggId")
        val reference = command.copy()
        fakePasswordService(
            instance = passwordService,
            withEggs = emptyList(),
        )
        val captureSystemOut = CapturedOutputPrintStream.captureSystemOut()

        // when
        expectThat(command) isEqualTo reference
        captureSystemOut.during {
            inputHandler.handleInput(inputOf(command))
        }

        // then
        expectThat(captureSystemOut.capture) isEqualTo "\n"
        expectThat(command) isNotEqualTo reference
    }
}
