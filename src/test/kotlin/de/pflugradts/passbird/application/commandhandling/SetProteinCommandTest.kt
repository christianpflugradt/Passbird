package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.SetProteinCommandHandler
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
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
class SetProteinCommandTest {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val configuration = mockk<Configuration>()
    private val passwordService = mockk<PasswordService>()
    private val setProteinCommandHandler = SetProteinCommandHandler(configuration, passwordService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(setProteinCommandHandler)

    @Test
    fun `should handle set protein command`() {
        // given
        val args = "EggId"
        val givenSlot = Slot.S1
        val shell = shellOf("p+${givenSlot.index()}$args")
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
            passwordService.putProtein(eq(shellOf(args)), givenSlot, eq(shellOf(givenType)), eq(shellOf(givenStructure)))
        }
        expectThat(shell) isNotEqualTo reference
    }
}
