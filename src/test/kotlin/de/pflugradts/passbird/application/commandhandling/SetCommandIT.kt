package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.SetCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.password.createPasswordEntryForTesting
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.provider.PasswordProvider
import de.pflugradts.passbird.domain.service.password.provider.fakePasswordProvider
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

class SetCommandIT {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val configuration = mockk<Configuration>()
    private val passwordService = mockk<PasswordService>()
    private val passwordProvider = mockk<PasswordProvider>()
    private val setCommandHandler = SetCommandHandler(configuration, passwordService, passwordProvider, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(setCommandHandler)

    @Test
    fun `should handle set command`() {
        // given
        val args = "key"
        val bytes = bytesOf("s$args")
        val reference = bytes.copy()
        val generatedPassword = bytesOf("p4s5w0rD")
        fakePasswordProvider(instance = passwordProvider, withCreatedPassword = generatedPassword)
        fakePasswordService(instance = passwordService)
        fakeConfiguration(instance = configuration)

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        verify(exactly = 1) { passwordService.putPasswordEntry(eq(bytesOf(args)), generatedPassword) }
        expectThat(bytes) isNotEqualTo reference
    }

    @Test
    fun `should handle set command with invalid alias`() {
        // given
        val args = "invalidkey1!"
        val bytes = bytesOf("s$args")
        val reference = bytes.copy()
        fakePasswordService(instance = passwordService, withInvalidAlias = true)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort)
        fakeConfiguration(instance = configuration)

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        val expectedOutput = "Password alias cannot contain digits or special characters. Please choose a different alias."
        verify(exactly = 1) { userInterfaceAdapterPort.send(eq(outputOf(bytesOf(expectedOutput)))) }
        verify(exactly = 0) { passwordService.putPasswordEntry(eq(bytesOf(args)), any()) }
        expectThat(bytes) isNotEqualTo reference
    }

    @Test
    fun `should handle set command with prompt on removal and new password entry`() {
        // given
        val args = "key"
        val bytes = bytesOf("s$args")
        val reference = bytes.copy()
        val generatedPassword = bytesOf("p4s5w0rD")
        fakePasswordProvider(instance = passwordProvider, withCreatedPassword = generatedPassword)
        fakePasswordService(instance = passwordService)
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        verify(exactly = 1) { passwordService.putPasswordEntry(eq(bytesOf(args)), generatedPassword) }
        expectThat(bytes) isNotEqualTo reference
    }

    @Test
    fun `should handle set command with prompt on removal and existing password entry`() {
        // given
        val args = "key"
        val bytes = bytesOf("s$args")
        val reference = bytes.copy()
        val generatedPassword = bytesOf("p4s5w0rD")
        val givenPasswordEntry = createPasswordEntryForTesting(withKeyBytes = bytesOf(args))
        fakePasswordProvider(instance = passwordProvider, withCreatedPassword = generatedPassword)
        fakePasswordService(instance = passwordService, withPasswordEntries = listOf(givenPasswordEntry))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveConfirmation = true)
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        verify(exactly = 1) { passwordService.putPasswordEntry(eq(bytesOf(args)), generatedPassword) }
        expectThat(bytes) isNotEqualTo reference
    }

    @Test
    fun `should handle set command with prompt on removal and operation aborted`() {
        // given
        val args = "key"
        val bytes = bytesOf("s$args")
        val reference = bytes.copy()
        val givenPasswordEntry = createPasswordEntryForTesting(withKeyBytes = bytesOf(args))
        fakePasswordService(instance = passwordService, withPasswordEntries = listOf(givenPasswordEntry))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveConfirmation = false)
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        verify(exactly = 0) { passwordService.putPasswordEntry(eq(bytesOf(args)), any()) }
        verify(exactly = 1) { userInterfaceAdapterPort.send(eq(outputOf(bytesOf("Operation aborted.")))) }
        expectThat(bytes) isNotEqualTo reference
    }
}