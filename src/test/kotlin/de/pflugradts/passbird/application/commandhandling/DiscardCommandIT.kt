package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.DiscardCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
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

class DiscardCommandIT {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val configuration = mockk<Configuration>()
    private val passwordService = mockk<PasswordService>()
    private val discardCommandHandler = DiscardCommandHandler(configuration, passwordService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(discardCommandHandler)

    @Test
    fun `should handle discard command`() {
        // given
        val args = "eggId"
        val bytes = bytesOf("d$args")
        val reference = bytes.copy()
        val givenEgg = createEggForTesting(withEggIdBytes = bytesOf(args))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeConfiguration(instance = configuration)

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        verify(exactly = 1) { passwordService.discardEgg(eq(bytesOf(args))) }
        expectThat(bytes) isNotEqualTo reference
    }

    @Test
    fun `should handle discard command with prompt on removal`() {
        // given
        val args = "eggId"
        val bytes = bytesOf("d$args")
        val reference = bytes.copy()
        val givenEgg = createEggForTesting(withEggIdBytes = bytesOf(args))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveConfirmation = true)
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        verify(exactly = 1) { passwordService.discardEgg(eq(bytesOf(args))) }
        expectThat(bytes) isNotEqualTo reference
    }

    @Test
    fun `should handle discard command with prompt on removal and operation aborted`() {
        // given
        val args = "eggId"
        val bytes = bytesOf("d$args")
        val reference = bytes.copy()
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveConfirmation = false)
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        verify(exactly = 0) { passwordService.discardEgg(eq(bytesOf(args))) }
        verify(exactly = 1) { userInterfaceAdapterPort.send(eq(outputOf(bytesOf("Operation aborted.")))) }
        expectThat(bytes) isNotEqualTo reference
    }
}
