package de.pflugradts.passbird.application.commandhandling.egg

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.createInputHandlerFor
import de.pflugradts.passbird.application.commandhandling.handler.egg.OneTimeSetCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.egg.PasswordRequirements
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.provider.PasswordProvider
import de.pflugradts.passbird.domain.service.password.provider.fakePasswordProvider
import io.mockk.Called
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue

@Tag(INTEGRATION)
class OneTimeSetCommandTest {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val configuration = mockk<Configuration>()
    private val passwordService = mockk<PasswordService>()
    private val passwordProvider = mockk<PasswordProvider>()
    private val oneTimeSetCommandHandler =
        OneTimeSetCommandHandler(configuration, passwordService, passwordProvider, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(oneTimeSetCommandHandler)

    @Test
    fun `should handle one-time set command`() {
        // given
        val args = "EggId"
        val shell = shellOf("s*$args")
        val reference = shell.copy()
        val generatedPassword = shellOf("p4s5w0rD")
        fakePasswordProvider(instance = passwordProvider, withCreatedPassword = generatedPassword)
        fakePasswordService(instance = passwordService)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(
                inputOf(shellOf("24")),
                inputOf(shellOf("!\"")),
            ),
            withTheseYesInputs = listOf(true, true, false, true),
        )
        fakeConfiguration(instance = configuration)
        val passwordRequirementsSlot = slot<PasswordRequirements>()

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify { passwordProvider.createNewPassword(capture(passwordRequirementsSlot)) }
        expectThat(passwordRequirementsSlot.isCaptured).isTrue()
        expectThat(passwordRequirementsSlot.captured.length) isEqualTo 24
        expectThat(passwordRequirementsSlot.captured.hasNumbers).isTrue()
        expectThat(passwordRequirementsSlot.captured.hasLowercaseLetters).isTrue()
        expectThat(passwordRequirementsSlot.captured.hasUppercaseLetters) isEqualTo false
        expectThat(passwordRequirementsSlot.captured.hasSpecialCharacters).isTrue()
        expectThat(passwordRequirementsSlot.captured.unusedSpecialCharacters) isEqualTo "!\""
        verify(exactly = 1) { passwordService.putEgg(eq(shellOf(args)), generatedPassword) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle one-time set command with invalid eggId`() {
        // given
        val args = "invalideggId1!"
        val shell = shellOf("s*$args")
        val reference = shell.copy()
        fakePasswordService(instance = passwordService, withInvalidEggId = true)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort)
        fakeConfiguration(instance = configuration)

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
    fun `should handle one-time set command with empty password length entered`() {
        // given
        val args = "EggId"
        val shell = shellOf("s*$args")
        val reference = shell.copy()
        fakePasswordService(instance = passwordService)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(emptyShell())))
        fakeConfiguration(instance = configuration)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { userInterfaceAdapterPort.send(outputOf(shellOf("Empty input - Operation aborted."))) }
        verify { passwordProvider wasNot Called }
        verify(exactly = 0) { passwordService.putEgg(eq(shellOf(args)), any()) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle one-time set command with invalid password requirements`() {
        // given
        val args = "EggId"
        val shell = shellOf("s*$args")
        val reference = shell.copy()
        fakePasswordService(instance = passwordService)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf("1"))),
            withTheseYesInputs = listOf(true, true, true, false),
        )
        fakeConfiguration(instance = configuration)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) {
            userInterfaceAdapterPort.send(eq(outputOf(shellOf("Specified configuration is invalid - Operation aborted."))))
        }
        verify { passwordProvider wasNot Called }
        verify(exactly = 0) { passwordService.putEgg(eq(shellOf(args)), any()) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle one-time set command with prompt on removal and existing egg`() {
        // given
        val args = "EggId"
        val shell = shellOf("s*$args")
        val reference = shell.copy()
        val generatedPassword = shellOf("p4s5w0rD")
        val givenEgg = createEggForTesting(withEggIdShell = shellOf(args))
        fakePasswordProvider(instance = passwordProvider, withCreatedPassword = generatedPassword)
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf("24"))),
            withTheseYesInputs = listOf(true, true, true, false),
            withReceiveConfirmation = true,
        )
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { passwordService.putEgg(eq(shellOf(args)), generatedPassword) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle one-time set command with prompt on removal and operation aborted`() {
        // given
        val args = "EggId"
        val shell = shellOf("s*$args")
        val reference = shell.copy()
        val givenEgg = createEggForTesting(withEggIdShell = shellOf(args))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf("24"))),
            withTheseYesInputs = listOf(true, true, true, false),
            withReceiveConfirmation = false,
        )
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
