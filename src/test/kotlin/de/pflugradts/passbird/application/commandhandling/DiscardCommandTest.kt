package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.egg.DiscardCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
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
class DiscardCommandTest {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val configuration = mockk<Configuration>()
    private val passwordService = mockk<PasswordService>()
    private val discardCommandHandler = DiscardCommandHandler(configuration, passwordService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(discardCommandHandler)

    @Test
    fun `should handle discard command`() {
        // given
        val args = "EggId"
        val shell = shellOf("d$args")
        val reference = shell.copy()
        val givenEgg = createEggForTesting(withEggIdShell = shellOf(args))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeConfiguration(instance = configuration)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { passwordService.discardEgg(eq(shellOf(args))) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle discard command for non existing egg`() {
        // given
        val args = "EggId"
        val shell = shellOf("d$args")
        val reference = shell.copy()
        fakePasswordService(instance = passwordService, withEggs = emptyList())
        fakeConfiguration(instance = configuration)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { passwordService.viewPassword(eq(shellOf(args))) }
        verify(exactly = 0) { passwordService.discardEgg(eq(shellOf(args))) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle discard command with prompt on removal`() {
        // given
        val args = "EggId"
        val shell = shellOf("d$args")
        val reference = shell.copy()
        val givenEgg = createEggForTesting(withEggIdShell = shellOf(args))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveConfirmation = true)
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { passwordService.discardEgg(eq(shellOf(args))) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle discard command with prompt on removal and operation aborted`() {
        // given
        val args = "EggId"
        val shell = shellOf("d$args")
        val reference = shell.copy()
        val givenEgg = createEggForTesting(withEggIdShell = shellOf(args))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveConfirmation = false)
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 0) { passwordService.discardEgg(eq(shellOf(args))) }
        verify(exactly = 1) { userInterfaceAdapterPort.send(eq(outputOf(shellOf("Operation aborted.")))) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should not prompt for removal for non existing egg`() {
        // given
        val args = "EggId"
        val shell = shellOf("d$args")
        val reference = shell.copy()
        fakePasswordService(instance = passwordService, withEggs = emptyList())
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { passwordService.viewPassword(eq(shellOf(args))) }
        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
        verify(exactly = 0) { userInterfaceAdapterPort.receiveConfirmation(any()) }
        verify(exactly = 0) { passwordService.discardEgg(eq(shellOf(args))) }
        expectThat(shell) isNotEqualTo reference
    }
}
