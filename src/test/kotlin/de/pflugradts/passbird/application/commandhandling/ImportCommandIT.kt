package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.ImportCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.exchange.ImportExportService
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.password.createPasswordEntryForTesting
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import java.util.stream.Stream

class ImportCommandIT {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val importExportService = mockk<ImportExportService>(relaxed = true)
    private val configuration = mockk<Configuration>()
    private val passwordService = mockk<PasswordService>()
    private val importCommandHandler = ImportCommandHandler(configuration, importExportService, passwordService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(importCommandHandler)

    @Test
    fun `should handle import command`() {
        // given
        val args = "tmp"
        val bytes = bytesOf("i$args")
        val reference = bytes.copy()
        fakeConfiguration(instance = configuration)

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        verify(exactly = 1) { importExportService.importPasswordEntries(args) }
        expectThat(bytes) isNotEqualTo reference
    }

    @Test
    fun `should handle import command with prompt on removal but no overlapping entries`() {
        // given
        val args = "tmp"
        val bytes = bytesOf("i$args")
        val reference = bytes.copy()
        val importKey1 = bytesOf("import1")
        val importKey2 = bytesOf("import2")
        val databaseKey1 = bytesOf("database1")
        val databaseKey2 = bytesOf("database2")
        val givenPasswordEntry1 = createPasswordEntryForTesting(withKeyBytes = databaseKey1)
        val givenPasswordEntry2 = createPasswordEntryForTesting(withKeyBytes = databaseKey2)
        every { importExportService.peekImportKeyBytes(args) } returns Stream.of(importKey1, importKey2)
        fakePasswordService(instance = passwordService, withPasswordEntries = listOf(givenPasswordEntry1, givenPasswordEntry2))
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        verify(exactly = 1) { importExportService.importPasswordEntries(args) }
        expectThat(bytes) isNotEqualTo reference
    }

    @Test
    fun `should handle import command with prompt on removal and overlapping entries`() {
        // given
        val args = "tmp"
        val bytes = bytesOf("i$args")
        val reference = bytes.copy()
        val importKey1 = bytesOf("import1")
        val importKey2 = bytesOf("overlap")
        val databaseKey1 = bytesOf("database1")
        val databaseKey2 = bytesOf("overlap")
        val givenPasswordEntry1 = createPasswordEntryForTesting(withKeyBytes = databaseKey1)
        val givenPasswordEntry2 = createPasswordEntryForTesting(withKeyBytes = databaseKey2)
        every { importExportService.peekImportKeyBytes(args) } returns Stream.of(importKey1, importKey2)
        fakePasswordService(instance = passwordService, withPasswordEntries = listOf(givenPasswordEntry1, givenPasswordEntry2))
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveConfirmation = true)

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        verify(exactly = 1) { importExportService.importPasswordEntries(args) }
        expectThat(bytes) isNotEqualTo reference
    }

    @Test
    fun `should handle import command with prompt on removal and operation aborted`() {
        // given
        val args = "tmp"
        val bytes = bytesOf("i$args")
        val reference = bytes.copy()
        val importKey1 = bytesOf("import1")
        val importKey2 = bytesOf("overlap")
        val databaseKey1 = bytesOf("database1")
        val databaseKey2 = bytesOf("overlap")
        val givenPasswordEntry1 = createPasswordEntryForTesting(withKeyBytes = databaseKey1)
        val givenPasswordEntry2 = createPasswordEntryForTesting(withKeyBytes = databaseKey2)
        every { importExportService.peekImportKeyBytes(args) } returns Stream.of(importKey1, importKey2)
        fakePasswordService(instance = passwordService, withPasswordEntries = listOf(givenPasswordEntry1, givenPasswordEntry2))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveConfirmation = false)
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        verify(exactly = 0) { importExportService.importPasswordEntries(args) }
        expectThat(bytes) isNotEqualTo reference
    }
}
