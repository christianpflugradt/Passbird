package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.ImportCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.exchange.ImportExportService
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.nest.NestSlot.DEFAULT
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag(INTEGRATION)
class ImportCommandTest {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val importExportService = mockk<ImportExportService>(relaxed = true)
    private val configuration = mockk<Configuration>()
    private val passwordService = mockk<PasswordService>()
    private val importCommandHandler = ImportCommandHandler(configuration, importExportService, passwordService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(importCommandHandler)

    @Test
    fun `should handle import command`() {
        // given
        val shell = shellOf("i")
        fakeConfiguration(instance = configuration)

        // when
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { importExportService.importEggs() }
    }

    @Test
    fun `should handle import command with prompt on removal but no overlapping entries`() {
        // given
        val shell = shellOf("i")
        val importEggId1 = shellOf("import1")
        val importEggId2 = shellOf("import2")
        val databaseEggId1 = shellOf("database1")
        val databaseEggId2 = shellOf("database2")
        val givenEgg1 = createEggForTesting(withEggIdShell = databaseEggId1)
        val givenEgg2 = createEggForTesting(withEggIdShell = databaseEggId2)
        every { importExportService.peekImportEggIdShells() } returns mapOf(DEFAULT to listOf(importEggId1, importEggId2))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg1, givenEgg2))
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { importExportService.importEggs() }
    }

    @Test
    fun `should handle import command with prompt on removal and overlapping entries`() {
        // given
        val shell = shellOf("i")
        val importEggId1 = shellOf("import1")
        val importEggId2 = shellOf("overlap")
        val databaseEggId1 = shellOf("database1")
        val databaseEggId2 = shellOf("overlap")
        val givenEgg1 = createEggForTesting(withEggIdShell = databaseEggId1)
        val givenEgg2 = createEggForTesting(withEggIdShell = databaseEggId2)
        every { importExportService.peekImportEggIdShells() } returns mapOf(DEFAULT to listOf(importEggId1, importEggId2))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg1, givenEgg2))
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveConfirmation = true)

        // when
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { importExportService.importEggs() }
    }

    @Test
    fun `should handle import command with prompt on removal and operation aborted`() {
        // given
        val shell = shellOf("i")
        val importEggId1 = shellOf("import1")
        val importEggId2 = shellOf("overlap")
        val databaseEggId1 = shellOf("database1")
        val databaseEggId2 = shellOf("overlap")
        val givenEgg1 = createEggForTesting(withEggIdShell = databaseEggId1)
        val givenEgg2 = createEggForTesting(withEggIdShell = databaseEggId2)
        every { importExportService.peekImportEggIdShells() } returns mapOf(DEFAULT to listOf(importEggId1, importEggId2))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg1, givenEgg2))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveConfirmation = false)
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 0) { importExportService.importEggs() }
    }

    // FIXME add tests for eggIds across multiple nests
}
