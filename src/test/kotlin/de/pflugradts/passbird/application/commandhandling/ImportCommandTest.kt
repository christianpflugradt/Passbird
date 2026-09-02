package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream.Companion.captureSystemErr
import de.pflugradts.kotlinextensions.TryResult.Companion.failure
import de.pflugradts.kotlinextensions.TryResult.Companion.success
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.ImportCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.exchange.ImportExportService
import de.pflugradts.passbird.application.exchange.ImportNestPreview
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.slot.Slot.S2
import de.pflugradts.passbird.domain.model.slot.Slot.S9
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.nest.createNestServiceForTesting
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction.DO_NOTHING
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Tag(INTEGRATION)
class ImportCommandTest {

    @BeforeEach
    fun setup() {
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(inputOf(shellOf("exchange password"))),
        )
    }

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val importExportService = mockk<ImportExportService>(relaxed = true)
    private val configuration = mockk<Configuration>()
    private val nestService = createNestServiceForTesting()
    private val passwordService = mockk<PasswordService>()
    private val commandExecutionTracker = CommandExecutionTracker()
    private val importCommandHandler = ImportCommandHandler(
        configuration,
        importExportService,
        nestService,
        passwordService,
        userInterfaceAdapterPort,
        commandExecutionTracker,
    )
    private val inputHandler = createInputHandlerFor(importCommandHandler, commandExecutionTracker)

    @Test
    fun `should handle import command`() {
        // given
        val shell = shellOf("i")
        fakeConfiguration(instance = configuration)

        // when
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { importExportService.importEggs(any()) }
    }

    @Test
    fun `should handle selective import command into an empty target nest slot`() {
        // given
        every { importExportService.peekImportNests(any()) } returns success(listOf(importPreview(slot = S9, nestId = "work", "import1")))
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(inputOf(shellOf("exchange password"))),
            withTheseInputs = listOf(inputOf(shellOf("9")), inputOf(shellOf("2"))),
        )
        fakePasswordService(instance = passwordService)
        fakeConfiguration(instance = configuration)

        // when
        inputHandler.handleInput(inputOf(shellOf("i*")))

        // then
        verify(exactly = 1) { importExportService.importEggs(S9, S2, any()) }
        verify(exactly = 1) { userInterfaceAdapterPort.send(eq(outputOf(shellOf("    9:   work")))) }
    }

    @Test
    fun `should confirm selective import using overlaps from the target nest slot`() {
        // given
        val overlappingEggId = shellOf("overlap")
        val givenEgg = createEggForTesting(withEggIdShell = overlappingEggId, withSlot = S2)
        every { importExportService.peekImportNests(any()) } returns success(listOf(importPreview(slot = S9, nestId = "work", "overlap")))
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(inputOf(shellOf("exchange password"))),
            withTheseInputs = listOf(inputOf(shellOf("9")), inputOf(shellOf("2"))),
            withReceiveConfirmation = true,
        )
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        inputHandler.handleInput(inputOf(shellOf("i*")))

        // then
        verify(exactly = 1) { passwordService.eggExists(any<Shell>(), S2) }
        verify(exactly = 1) { userInterfaceAdapterPort.receiveConfirmation(any()) }
        verify(exactly = 1) { importExportService.importEggs(S9, S2, any()) }
        verify(exactly = 0) { passwordService.eggExists(overlappingEggId, S9) }
    }

    @Test
    fun `should abort selective import when target slot is occupied by a different nest`() {
        // given
        nestService.place(shellOf("local"), S2)
        every { importExportService.peekImportNests(any()) } returns success(listOf(importPreview(slot = S9, nestId = "work", "import1")))
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(inputOf(shellOf("exchange password"))),
            withTheseInputs = listOf(inputOf(shellOf("9")), inputOf(shellOf("2"))),
        )
        fakeConfiguration(instance = configuration)

        // when
        inputHandler.handleInput(inputOf(shellOf("i*")))

        // then
        verify(exactly = 0) { importExportService.importEggs(any(), any(), any()) }
    }

    @Test
    fun `should abort selective import into default slot for a non default nest`() {
        // given
        every { importExportService.peekImportNests(any()) } returns success(listOf(importPreview(slot = S9, nestId = "work", "import1")))
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(inputOf(shellOf("exchange password"))),
            withTheseInputs = listOf(inputOf(shellOf("9")), inputOf(shellOf("0"))),
        )
        fakeConfiguration(instance = configuration)

        // when
        inputHandler.handleInput(inputOf(shellOf("i*")))

        // then
        verify(exactly = 0) { importExportService.importEggs(any(), any(), any()) }
    }

    @Test
    fun `should scramble selective import preview shells after target selection`() {
        // given
        val nestId = spyk(shellOf("work"))
        val importEggId = spyk(shellOf("import1"))
        every { importExportService.peekImportNests(any()) } returns success(
            listOf(ImportNestPreview(nestId = nestId, slot = S9, eggIds = listOf(importEggId))),
        )
        every { passwordService.eggExists(any(), S2) } returns false
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(inputOf(shellOf("exchange password"))),
            withTheseInputs = listOf(inputOf(shellOf("9")), inputOf(shellOf("2"))),
        )
        fakeConfiguration(instance = configuration)

        // when
        inputHandler.handleInput(inputOf(shellOf("i*")))

        // then
        verify(exactly = 1) { importExportService.importEggs(S9, S2, any()) }
        verify(exactly = 1) { nestId.scramble() }
        verify(exactly = 1) { importEggId.scramble() }
    }

    @Test
    fun `should handle import command with prompt on removal but no overlapping entries`() {
        // given
        val shell = shellOf("i")
        val importEggId1 = shellOf("import1")
        val importEggId2 = shellOf("import2")
        val treeEggId1 = shellOf("tree1")
        val treeEggId2 = shellOf("tree2")
        val givenEgg1 = createEggForTesting(withEggIdShell = treeEggId1)
        val givenEgg2 = createEggForTesting(withEggIdShell = treeEggId2)
        every { importExportService.peekImportEggIdShells(any()) } returns success(mapOf(DEFAULT to listOf(importEggId1, importEggId2)))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg1, givenEgg2))
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { importExportService.importEggs(any()) }
    }

    @Test
    fun `should scramble full import preview eggIds after overlap check`() {
        // given
        val shell = shellOf("i")
        val importEggId1 = spyk(shellOf("import1"))
        val importEggId2 = spyk(shellOf("import2"))
        every { importExportService.peekImportEggIdShells(any()) } returns success(mapOf(DEFAULT to listOf(importEggId1, importEggId2)))
        every { passwordService.eggExists(any(), DEFAULT) } returns false
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { importExportService.importEggs(any()) }
        verify(exactly = 1) { importEggId1.scramble() }
        verify(exactly = 1) { importEggId2.scramble() }
    }

    @Test
    fun `should handle import command with prompt on removal and overlapping entries`() {
        // given
        val shell = shellOf("i")
        val importEggId1 = shellOf("import1")
        val importEggId2 = shellOf("overlap")
        val treeEggId1 = shellOf("tree1")
        val treeEggId2 = shellOf("overlap")
        val givenEgg1 = createEggForTesting(withEggIdShell = treeEggId1)
        val givenEgg2 = createEggForTesting(withEggIdShell = treeEggId2)
        every { importExportService.peekImportEggIdShells(any()) } returns success(mapOf(DEFAULT to listOf(importEggId1, importEggId2)))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg1, givenEgg2))
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(inputOf(shellOf("exchange password"))),
            withReceiveConfirmation = true,
        )

        // when
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { importExportService.importEggs(any()) }
    }

    @Test
    fun `should handle import command with prompt on removal and operation aborted`() {
        // given
        val shell = shellOf("i")
        val importEggId1 = shellOf("import1")
        val importEggId2 = shellOf("overlap")
        val treeEggId1 = shellOf("tree1")
        val treeEggId2 = shellOf("overlap")
        val givenEgg1 = createEggForTesting(withEggIdShell = treeEggId1)
        val givenEgg2 = createEggForTesting(withEggIdShell = treeEggId2)
        every { importExportService.peekImportEggIdShells(any()) } returns success(mapOf(DEFAULT to listOf(importEggId1, importEggId2)))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg1, givenEgg2))
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(inputOf(shellOf("exchange password"))),
            withReceiveConfirmation = false,
        )
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 0) { importExportService.importEggs(any()) }
    }

    @Test
    fun `should not prompt or import when import preview fails`() {
        // given
        val shell = shellOf("i")
        every { importExportService.peekImportEggIdShells(any()) } returns failure(IllegalStateException("preview failed"))
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 0) { userInterfaceAdapterPort.receiveConfirmation(any()) }
        verify(exactly = 0) { importExportService.importEggs(any()) }
        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
    }

    @Test
    fun `should reject import command with trailing input`() {
        // given
        fakeConfiguration(instance = configuration)
        val captureSystemErr = captureSystemErr()

        // when
        captureSystemErr.during {
            inputHandler.handleInput(inputOf(shellOf("iimport")))
        }

        // then
        verify(exactly = 0) { importExportService.importEggs(any()) }
        verify(exactly = 0) { importExportService.peekImportEggIdShells(any()) }
        expectThat(captureSystemErr.capture) isEqualTo "Command execution failed: Parameter for command 'i' not supported: import\n"
    }

    // FIXME add tests for eggIds across multiple nests
}

private fun importPreview(slot: de.pflugradts.passbird.domain.model.slot.Slot, nestId: String, vararg eggIds: String) = ImportNestPreview(
    nestId = shellOf(nestId),
    slot = slot,
    eggIds = eggIds.map(::shellOf),
)
