package de.pflugradts.passbird.application.commandhandling.protein

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.createInputHandlerFor
import de.pflugradts.passbird.application.commandhandling.handler.protein.DiscardProteinCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot.S1
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
class DiscardProteinCommandTest {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val configuration = mockk<Configuration>()
    private val passwordService = mockk<PasswordService>()
    private val discardProteinCommandHandler = DiscardProteinCommandHandler(configuration, passwordService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(discardProteinCommandHandler)

    @Test
    fun `should handle discard protein command`() {
        // given
        val args = "EggId"
        val slot = S1
        val shell = shellOf("p-${slot.index()}$args")
        val reference = shell.copy()
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(
                createEggForTesting(
                    withEggIdShell = shellOf(args),
                    withProteins = mapOf(slot to Pair(shellOf("type"), shellOf("structure"))),
                ),
            ),
        )
        fakeConfiguration(instance = configuration)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { passwordService.discardProtein(eq(shellOf(args)), slot) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle discard protein command with non existing egg`() {
        // given
        val args = "EggId"
        val slot = S1
        val shell = shellOf("p-${slot.index()}$args")
        val reference = shell.copy()
        fakePasswordService(instance = passwordService, withEggs = emptyList())
        fakeConfiguration(instance = configuration)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 0) { passwordService.discardProtein(eq(shellOf(args)), slot) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle discard protein command with prompt on removal`() {
        // given
        val args = "EggId"
        val slot = S1
        val shell = shellOf("p-${slot.index()}$args")
        val reference = shell.copy()
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(
                createEggForTesting(
                    withEggIdShell = shellOf(args),
                    withProteins = mapOf(slot to Pair(shellOf("type"), shellOf("structure"))),
                ),
            ),
        )
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveConfirmation = true)
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { passwordService.discardProtein(eq(shellOf(args)), slot) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle discard protein command with prompt on removal and operation aborted`() {
        // given
        val args = "EggId"
        val slot = S1
        val shell = shellOf("p-${slot.index()}$args")
        val reference = shell.copy()
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(
                createEggForTesting(
                    withEggIdShell = shellOf(args),
                    withProteins = mapOf(slot to Pair(shellOf("type"), shellOf("structure"))),
                ),
            ),
        )
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveConfirmation = false)
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 0) { passwordService.discardProtein(eq(shellOf(args)), slot) }
        verify(exactly = 1) { userInterfaceAdapterPort.send(eq(outputOf(shellOf("Operation aborted.")))) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should not prompt on removal for non existing egg`() {
        // given
        val args = "EggId"
        val slot = S1
        val shell = shellOf("p-${slot.index()}$args")
        val reference = shell.copy()
        fakePasswordService(instance = passwordService, withEggs = emptyList())
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 0) { passwordService.discardProtein(eq(shellOf(args)), slot) }
        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
        verify(exactly = 0) { userInterfaceAdapterPort.receiveConfirmation(any()) }
        expectThat(shell) isNotEqualTo reference
    }
}
