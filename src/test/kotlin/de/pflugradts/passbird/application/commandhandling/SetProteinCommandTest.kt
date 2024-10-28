package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.SetProteinCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

@Tag(INTEGRATION)
class SetProteinCommandTest {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val configuration = mockk<Configuration>()
    private val passwordService = mockk<PasswordService>()
    private val setProteinCommandHandler = SetProteinCommandHandler(configuration, passwordService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(setProteinCommandHandler)

    @ParameterizedTest
    @EnumSource(value = Slot::class)
    fun `should handle set protein command`(slot: Slot) {
        // given
        val args = "EggId"
        val shell = shellOf("p+${slot.index()}$args")
        val reference = shell.copy()
        val givenEgg = createEggForTesting(withEggIdShell = shellOf(args))
        val givenType = "url"
        val givenStructure = "example.com"
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeConfiguration(instance = configuration)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf(givenType))),
            withTheseSecureInputs = listOf(inputOf(shellOf(givenStructure))),
        )

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) {
            passwordService.putProtein(eq(shellOf(args)), slot, eq(shellOf(givenType)), eq(shellOf(givenStructure)))
        }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle set protein command with invalid egg`() {
        // given
        val args = "EggId"
        val command = shellOf("p+0$args")
        val reference = command.copy()
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(createEggForTesting(withEggIdShell = shellOf("other"))),
        )

        // when
        expectThat(command) isEqualTo reference
        inputHandler.handleInput(inputOf(command))

        // then
        verify(exactly = 0) { passwordService.putProtein(shellOf(args), any(), any(), any()) }
        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
        expectThat(command) isNotEqualTo reference
    }

    @Test
    fun `should handle set protein command with prompt on removal and new protein`() {
        // given
        val args = "EggId"
        val slot = DEFAULT
        val shell = shellOf("p+${slot.index()}$args")
        val reference = shell.copy()
        val givenEgg = createEggForTesting(withEggIdShell = shellOf(args))
        val givenType = "url"
        val givenStructure = "example.com"
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf(givenType))),
            withTheseSecureInputs = listOf(inputOf(shellOf(givenStructure))),
        )

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) {
            passwordService.putProtein(eq(shellOf(args)), slot, eq(shellOf(givenType)), eq(shellOf(givenStructure)))
        }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle set protein command with prompt on removal and existing protein`() {
        // given
        val args = "EggId"
        val slot = DEFAULT
        val shell = shellOf("p+${slot.index()}$args")
        val reference = shell.copy()
        val givenEgg = createEggForTesting(
            withEggIdShell = shellOf(args),
            withProteins = mapOf(slot to ShellPair(shellOf("url"), shellOf("example.com"))),
        )
        val givenType = "url"
        val givenStructure = "example.com"
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withReceiveConfirmation = true,
            withTheseInputs = listOf(inputOf(shellOf(givenType))),
            withTheseSecureInputs = listOf(inputOf(shellOf(givenStructure))),
        )

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) {
            passwordService.putProtein(eq(shellOf(args)), slot, eq(shellOf(givenType)), eq(shellOf(givenStructure)))
        }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle set protein command with prompt on removal and operation aborted`() {
        // given
        val args = "EggId"
        val slot = DEFAULT
        val shell = shellOf("p+${slot.index()}$args")
        val reference = shell.copy()
        val givenEgg = createEggForTesting(
            withEggIdShell = shellOf(args),
            withProteins = mapOf(slot to ShellPair(shellOf("url"), shellOf("example.com"))),
        )
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeConfiguration(instance = configuration, withPromptOnRemoval = true)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withReceiveConfirmation = false)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 0) { passwordService.putProtein(eq(shellOf(args)), slot, any(), any()) }
        verify(exactly = 1) { userInterfaceAdapterPort.send(eq(outputOf(shellOf("Operation aborted.")))) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle set protein command with new protein and no type provided`() {
        // given
        val args = "EggId"
        val slot = DEFAULT
        val shell = shellOf("p+${slot.index()}$args")
        val reference = shell.copy()
        val givenEgg = createEggForTesting(withEggIdShell = shellOf(args))
        val givenType = ""
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeConfiguration(instance = configuration)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf(givenType))),
        )

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { userInterfaceAdapterPort.send(eq(outputOf(shellOf("Operation aborted.")))) }
        verify(exactly = 0) { passwordService.putProtein(eq(shellOf(args)), slot, any(), any()) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle set protein command with new protein and no structure provided`() {
        // given
        val args = "EggId"
        val slot = DEFAULT
        val shell = shellOf("p+${slot.index()}$args")
        val reference = shell.copy()
        val givenEgg = createEggForTesting(withEggIdShell = shellOf(args))
        val givenType = "url"
        val givenStructure = ""
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeConfiguration(instance = configuration)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf(givenType))),
            withTheseSecureInputs = listOf(inputOf(shellOf(givenStructure))),
        )

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { userInterfaceAdapterPort.send(eq(outputOf(shellOf("Operation aborted.")))) }
        verify(exactly = 0) { passwordService.putProtein(eq(shellOf(args)), slot, any(), any()) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle set protein command with existing protein and no type provided`() {
        // given
        val args = "EggId"
        val slot = DEFAULT
        val shell = shellOf("p+${slot.index()}$args")
        val reference = shell.copy()
        val givenPreviousType = "url"
        val givenEgg = createEggForTesting(
            withEggIdShell = shellOf(args),
            withProteins = mapOf(slot to ShellPair(shellOf(givenPreviousType), shellOf("example.com"))),
        )
        val givenType = ""
        val givenStructure = "example.org"
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeConfiguration(instance = configuration)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf(givenType))),
            withTheseSecureInputs = listOf(inputOf(shellOf(givenStructure))),
        )

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) {
            passwordService.putProtein(eq(shellOf(args)), slot, eq(shellOf(givenPreviousType)), eq(shellOf(givenStructure)))
        }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle set protein command with existing protein and no structure provided`() {
        // given
        val args = "EggId"
        val slot = DEFAULT
        val shell = shellOf("p+${slot.index()}$args")
        val reference = shell.copy()
        val givenEgg = createEggForTesting(
            withEggIdShell = shellOf(args),
            withProteins = mapOf(slot to ShellPair(shellOf("url"), shellOf("example.com"))),
        )
        val givenType = "url"
        val givenStructure = ""
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeConfiguration(instance = configuration)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf(givenType))),
            withTheseSecureInputs = listOf(inputOf(shellOf(givenStructure))),
        )

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { userInterfaceAdapterPort.send(eq(outputOf(shellOf("Operation aborted.")))) }
        verify(exactly = 0) { passwordService.putProtein(eq(shellOf(args)), slot, any(), any()) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle set protein command with existing protein and new type provided`() {
        // given
        val args = "EggId"
        val slot = DEFAULT
        val shell = shellOf("p+${slot.index()}$args")
        val reference = shell.copy()
        val givenPreviousType = "url"
        val givenEgg = createEggForTesting(
            withEggIdShell = shellOf(args),
            withProteins = mapOf(slot to ShellPair(shellOf(givenPreviousType), shellOf("example.com"))),
        )
        val givenType = "link"
        val givenStructure = "example.org"
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeConfiguration(instance = configuration)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseInputs = listOf(inputOf(shellOf(givenType))),
            withTheseSecureInputs = listOf(inputOf(shellOf(givenStructure))),
        )

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) {
            passwordService.putProtein(eq(shellOf(args)), slot, eq(shellOf(givenType)), eq(shellOf(givenStructure)))
        }
        expectThat(shell) isNotEqualTo reference
    }
}
