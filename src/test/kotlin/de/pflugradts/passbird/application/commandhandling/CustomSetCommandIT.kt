package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.CustomSetCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
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

class CustomSetCommandIT {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val configuration = mockk<Configuration>()
    private val passwordService = mockk<PasswordService>()
    private val customSetCommandHandler = CustomSetCommandHandler(configuration, passwordService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(customSetCommandHandler)

    @Test
    fun `should handle custom set command`() {
        // given
        val args = "eggId"
        val bytes = bytesOf("c$args")
        val reference = bytes.copy()
        val customPassword = mockk<Bytes>(relaxed = true)
        fakePasswordService(instance = passwordService)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseSecureInputs = listOf(inputOf(customPassword)))
        fakeConfiguration(instance = configuration)

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        verify(exactly = 1) { passwordService.putEgg(eq(bytesOf(args)), customPassword) }
        verify(exactly = 1) { customPassword.scramble() }
        expectThat(bytes) isNotEqualTo reference
    }

    @Test
    fun `should handle custom set command with invalid eggId`() {
        // given
        val args = "invalideggId1!"
        val bytes = bytesOf("c$args")
        val reference = bytes.copy()
        fakePasswordService(instance = passwordService, withInvalidEggId = true)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort)
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        val errorMsg = "Password alias cannot contain digits or special characters. Please choose a different alias."
        verify(exactly = 1) { userInterfaceAdapterPort.send(eq(outputOf(bytesOf(errorMsg)))) }
        verify(exactly = 0) { passwordService.putEgg(eq(bytesOf(args)), any()) }
        expectThat(bytes) isNotEqualTo reference
    }

    @Test
    fun `should handle custom set command with empty password entered`() {
        // given
        val args = "eggId"
        val bytes = bytesOf("c$args")
        val reference = bytes.copy()
        val customPassword = emptyBytes()
        fakePasswordService(instance = passwordService)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseSecureInputs = listOf(inputOf(customPassword)))
        fakeConfiguration(instance = configuration)

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        verify(exactly = 1) { userInterfaceAdapterPort.send(outputOf(bytesOf("Empty input - Operation aborted."))) }
        verify(exactly = 0) { passwordService.putEgg(eq(bytesOf(args)), any()) }
        expectThat(bytes) isNotEqualTo reference
    }

    @Test
    fun `should handle custom set command with prompt on removal and new egg`() {
        // given
        val args = "eggId"
        val bytes = bytesOf("c$args")
        val reference = bytes.copy()
        val customPassword = mockk<Bytes>(relaxed = true)
        fakePasswordService(instance = passwordService)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseSecureInputs = listOf(inputOf(customPassword)))
        fakeConfiguration(instance = configuration)

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        verify(exactly = 1) { passwordService.putEgg(eq(bytesOf(args)), customPassword) }
        verify(exactly = 1) { customPassword.scramble() }
        expectThat(bytes) isNotEqualTo reference
    }

    @Test
    fun `should handle custom set command with prompt on removal and existing egg`() {
        // given
        val args = "eggId"
        val bytes = bytesOf("c$args")
        val reference = bytes.copy()
        val customPassword = mockk<Bytes>(relaxed = true)
        val givenEgg = createEggForTesting(withEggIdBytes = bytesOf(args))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(inputOf(customPassword)),
            withReceiveConfirmation = true,
        )
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        verify(exactly = 1) { passwordService.putEgg(eq(bytesOf(args)), customPassword) }
        verify(exactly = 1) { customPassword.scramble() }
        expectThat(bytes) isNotEqualTo reference
    }

    @Test
    fun `should handle custom set command with prompt on removal and operation aborted`() {
        // given
        val args = "eggId"
        val bytes = bytesOf("c$args")
        val reference = bytes.copy()
        val givenEgg = createEggForTesting(withEggIdBytes = bytesOf(args))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveConfirmation = false)
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        verify(exactly = 1) { userInterfaceAdapterPort.send(outputOf(bytesOf("Operation aborted."))) }
        verify(exactly = 0) { passwordService.putEgg(eq(bytesOf(args)), any()) }
        expectThat(bytes) isNotEqualTo reference
    }
}
