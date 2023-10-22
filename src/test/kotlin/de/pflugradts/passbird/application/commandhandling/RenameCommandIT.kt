package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.RenameCommandHandler
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.password.createPasswordEntryForTesting
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

internal class RenameCommandIT {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val passwordService = mockk<PasswordService>()
    private val renameCommandHandler = RenameCommandHandler(passwordService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(renameCommandHandler)

    @Test
    fun `should handle rename command`() {
        // given
        val args = "key123"
        val bytes = bytesOf("r$args")
        val reference = bytes.copy()
        val newAlias = mockk<Bytes>(relaxed = true)
        val givenPasswordEntry = createPasswordEntryForTesting(withKeyBytes = bytesOf(args))
        fakePasswordService(instance = passwordService, withPasswordEntries = listOf(givenPasswordEntry))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(newAlias)))

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        verify(exactly = 1) { passwordService.renamePasswordEntry(eq(bytesOf(args)), newAlias) }
        verify(exactly = 1) { newAlias.scramble() }
        expectThat(givenPasswordEntry.viewKey()) isEqualTo reference.slice(1)
        expectThat(bytes) isNotEqualTo reference
    }

    @Test
    fun `should handle rename command with unknown alias`() {
        // given
        val args = "invalidkey1!"
        val bytes = bytesOf("r$args")
        val reference = bytes.copy()
        fakePasswordService(instance = passwordService, withInvalidAlias = true)

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        verify(exactly = 0) { passwordService.renamePasswordEntry(eq(bytesOf(args)), any()) }
        expectThat(bytes) isNotEqualTo reference
    }

    @Test
    fun `should handle rename command with empty alias entered`() {
        // given
        val args = "key123"
        val bytes = bytesOf("r$args")
        val reference = bytes.copy()
        val newAlias = bytesOf("")
        val givenPasswordEntry = createPasswordEntryForTesting(withKeyBytes = bytesOf(args))
        fakePasswordService(instance = passwordService, withPasswordEntries = listOf(givenPasswordEntry))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(newAlias)))

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        verify(exactly = 0) { passwordService.renamePasswordEntry(eq(bytesOf(args)), any()) }
        verify(exactly = 1) { userInterfaceAdapterPort.send(outputOf(bytesOf("Empty input - Operation aborted."))) }
        expectThat(bytes) isNotEqualTo reference
    }

    @Test
    fun `should handle rename command with existing alias entered`() {
        // given
        val args = "key123"
        val bytes = bytesOf("r$args")
        val reference = bytes.copy()
        val existingAlias = bytesOf("existingkey")
        val givenPasswordEntry = createPasswordEntryForTesting(withKeyBytes = bytesOf(args))
        fakePasswordService(instance = passwordService, withPasswordEntries = listOf(givenPasswordEntry))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(existingAlias)))

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        expectThat(givenPasswordEntry.viewKey()) isEqualTo reference.slice(1)
        expectThat(bytes) isNotEqualTo reference
    }
}
