package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.egg.RenameCommandHandler
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.shell.fakeDec
import de.pflugradts.passbird.domain.model.shell.fakeEnc
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
internal class RenameCommandTest {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val passwordService = mockk<PasswordService>()
    private val renameCommandHandler = RenameCommandHandler(passwordService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(renameCommandHandler)

    @Test
    fun `should handle rename command`() {
        // given
        val args = "EggId123"
        val shell = shellOf("r$args")
        val reference = shell.copy()
        val newEggId = mockk<Shell>(relaxed = true)
        val givenEgg = createEggForTesting(withEggIdShell = shellOf(args))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(newEggId)))

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 1) { passwordService.renameEgg(eq(shellOf(args)), newEggId) }
        verify(exactly = 1) { newEggId.scramble() }
        expectThat(givenEgg.viewEggId()) isEqualTo reference.slice(1).fakeEnc()
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle rename command with unknown eggId`() {
        // given
        val args = "invalideggId1!"
        val shell = shellOf("r$args")
        val reference = shell.copy()
        fakePasswordService(instance = passwordService, withInvalidEggId = true)

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 0) { passwordService.renameEgg(eq(shellOf(args)), any()) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle rename command with empty eggId entered`() {
        // given
        val args = "EggId123"
        val shell = shellOf("r$args")
        val reference = shell.copy()
        val newEggId = shellOf("")
        val givenEgg = createEggForTesting(withEggIdShell = shellOf(args))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(newEggId)))

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        verify(exactly = 0) { passwordService.renameEgg(eq(shellOf(args)), any()) }
        verify(exactly = 1) { userInterfaceAdapterPort.send(outputOf(shellOf("Empty input - Operation aborted."))) }
        expectThat(shell) isNotEqualTo reference
    }

    @Test
    fun `should handle rename command with existing eggId entered`() {
        // given
        val args = "EggId123"
        val shell = shellOf("r$args")
        val reference = shell.copy()
        val existingEggId = shellOf("existingeggId")
        val givenEgg = createEggForTesting(withEggIdShell = shellOf(args))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(existingEggId)))

        // when
        expectThat(shell) isEqualTo reference
        inputHandler.handleInput(inputOf(shell))

        // then
        expectThat(givenEgg.viewEggId().fakeDec()) isEqualTo reference.slice(1)
        expectThat(shell) isNotEqualTo reference
    }
}
