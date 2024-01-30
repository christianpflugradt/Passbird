package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.CustomSetCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

@Tag(INTEGRATION)
class CustomSetCommandTest {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val configuration = mockk<Configuration>()
    private val passwordService = mockk<PasswordService>()
    private val customSetCommandHandler = CustomSetCommandHandler(configuration, passwordService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(customSetCommandHandler)

    @Test
    fun `should handle custom set command`() {
        // given
        val args = "EggId"
        val shell = shellOf("c$args")
        val reference = shell.copy()
        val customPassword = mockk<Shell>(relaxed = true)
        fakePasswordService(instance = passwordService)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseSecureInputs = listOf(inputOf(customPassword)))
        fakeConfiguration(instance = configuration)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { passwordService.putEgg(eq(shellOf(args)), customPassword) }
        verify(exactly = 1) { customPassword.scramble() }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle custom set command with invalid eggId`() {
        // given
        val args = "invalideggId1!"
        val shell = shellOf("c$args")
        val reference = shell.copy()
        fakePasswordService(instance = passwordService, withInvalidEggId = true)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort)
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        val expectedOutput = "EggId '$args' contains non alphabetic characters - Operation aborted."
        verify(exactly = 1) { userInterfaceAdapterPort.send(eq(outputOf(shellOf(expectedOutput), OPERATION_ABORTED))) }
        verify(exactly = 0) { passwordService.putEgg(eq(shellOf(args)), any()) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle custom set command with empty password entered`() {
        // given
        val args = "EggId"
        val shell = shellOf("c$args")
        val reference = shell.copy()
        val customPassword = emptyShell()
        fakePasswordService(instance = passwordService)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseSecureInputs = listOf(inputOf(customPassword)))
        fakeConfiguration(instance = configuration)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { userInterfaceAdapterPort.send(outputOf(shellOf("Empty input - Operation aborted."))) }
        verify(exactly = 0) { passwordService.putEgg(eq(shellOf(args)), any()) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle custom set command with prompt on removal and new egg`() {
        // given
        val args = "EggId"
        val shell = shellOf("c$args")
        val reference = shell.copy()
        val customPassword = mockk<Shell>(relaxed = true)
        fakePasswordService(instance = passwordService)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseSecureInputs = listOf(inputOf(customPassword)))
        fakeConfiguration(instance = configuration)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { passwordService.putEgg(eq(shellOf(args)), customPassword) }
        verify(exactly = 1) { customPassword.scramble() }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle custom set command with prompt on removal and existing egg`() {
        // given
        val args = "EggId"
        val shell = shellOf("c$args")
        val reference = shell.copy()
        val customPassword = mockk<Shell>(relaxed = true)
        val givenEgg = createEggForTesting(withEggIdShell = shellOf(args))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(inputOf(customPassword)),
            withReceiveConfirmation = true,
        )
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { passwordService.putEgg(eq(shellOf(args)), customPassword) }
        verify(exactly = 1) { customPassword.scramble() }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle custom set command with prompt on removal and operation aborted`() {
        // given
        val args = "EggId"
        val shell = shellOf("c$args")
        val reference = shell.copy()
        val givenEgg = createEggForTesting(withEggIdShell = shellOf(args))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveConfirmation = false)
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."))) }
        verify(exactly = 0) { passwordService.putEgg(eq(shellOf(args)), any()) }
        expectThat(shell) isNotEqualTo reference
    }
}
