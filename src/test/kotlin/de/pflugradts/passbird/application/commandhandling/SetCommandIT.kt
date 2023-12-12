package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.SetCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
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
        val args = "eggId"
        val shell = shellOf("s$args")
        val reference = shell.copy()
        val generatedPassword = shellOf("p4s5w0rD")
        fakePasswordProvider(instance = passwordProvider, withCreatedPassword = generatedPassword)
        fakePasswordService(instance = passwordService)
        fakeConfiguration(instance = configuration)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { passwordService.putEgg(eq(shellOf(args)), generatedPassword) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle set command with invalid eggId`() {
        // given
        val args = "invalideggId1!"
        val shell = shellOf("s$args")
        val reference = shell.copy()
        fakePasswordService(instance = passwordService, withInvalidEggId = true)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort)
        fakeConfiguration(instance = configuration)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        val expectedOutput = "Password alias cannot contain digits or special characters. Please choose a different alias."
        verify(exactly = 1) { userInterfaceAdapterPort.send(eq(outputOf(shellOf(expectedOutput)))) }
        verify(exactly = 0) { passwordService.putEgg(eq(shellOf(args)), any()) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle set command with prompt on removal and new egg`() {
        // given
        val args = "eggId"
        val shell = shellOf("s$args")
        val reference = shell.copy()
        val generatedPassword = shellOf("p4s5w0rD")
        fakePasswordProvider(instance = passwordProvider, withCreatedPassword = generatedPassword)
        fakePasswordService(instance = passwordService)
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { passwordService.putEgg(eq(shellOf(args)), generatedPassword) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle set command with prompt on removal and existing egg`() {
        // given
        val args = "eggId"
        val shell = shellOf("s$args")
        val reference = shell.copy()
        val generatedPassword = shellOf("p4s5w0rD")
        val givenEgg = createEggForTesting(withEggIdShell = shellOf(args))
        fakePasswordProvider(instance = passwordProvider, withCreatedPassword = generatedPassword)
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveConfirmation = true)
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { passwordService.putEgg(eq(shellOf(args)), generatedPassword) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle set command with prompt on removal and operation aborted`() {
        // given
        val args = "eggId"
        val shell = shellOf("s$args")
        val reference = shell.copy()
        val givenEgg = createEggForTesting(withEggIdShell = shellOf(args))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveConfirmation = false)
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 0) { passwordService.putEgg(eq(shellOf(args)), any()) }
        verify(exactly = 1) { userInterfaceAdapterPort.send(eq(outputOf(shellOf("Operation aborted.")))) }
        expectThat(shell) isNotEqualTo reference
    }
}
