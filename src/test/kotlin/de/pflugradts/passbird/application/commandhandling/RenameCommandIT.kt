package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.RenameCommandHandler
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.transfer.Bytes
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

internal class RenameCommandIT {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val passwordService = mockk<PasswordService>()
    private val renameCommandHandler = RenameCommandHandler(passwordService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(renameCommandHandler)

    @Test
    fun `should handle rename command`() {
        // given
        val args = "eggId123"
        val bytes = bytesOf("r$args")
        val reference = bytes.copy()
        val newEggId = mockk<Bytes>(relaxed = true)
        val givenEgg = createEggForTesting(withEggIdBytes = bytesOf(args))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(newEggId)))

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        verify(exactly = 1) { passwordService.renameEgg(eq(bytesOf(args)), newEggId) }
        verify(exactly = 1) { newEggId.scramble() }
        expectThat(givenEgg.viewEggId()) isEqualTo reference.slice(1)
        expectThat(bytes) isNotEqualTo reference
    }

    @Test
    fun `should handle rename command with unknown eggId`() {
        // given
        val args = "invalideggId1!"
        val bytes = bytesOf("r$args")
        val reference = bytes.copy()
        fakePasswordService(instance = passwordService, withInvalidEggId = true)

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        verify(exactly = 0) { passwordService.renameEgg(eq(bytesOf(args)), any()) }
        expectThat(bytes) isNotEqualTo reference
    }

    @Test
    fun `should handle rename command with empty eggId entered`() {
        // given
        val args = "eggId123"
        val bytes = bytesOf("r$args")
        val reference = bytes.copy()
        val newEggId = bytesOf("")
        val givenEgg = createEggForTesting(withEggIdBytes = bytesOf(args))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(newEggId)))

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        verify(exactly = 0) { passwordService.renameEgg(eq(bytesOf(args)), any()) }
        verify(exactly = 1) { userInterfaceAdapterPort.send(outputOf(bytesOf("Empty input - Operation aborted."))) }
        expectThat(bytes) isNotEqualTo reference
    }

    @Test
    fun `should handle rename command with existing eggId entered`() {
        // given
        val args = "eggId123"
        val bytes = bytesOf("r$args")
        val reference = bytes.copy()
        val existingEggId = bytesOf("existingeggId")
        val givenEgg = createEggForTesting(withEggIdBytes = bytesOf(args))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(existingEggId)))

        // when
        expectThat(bytes) isEqualTo reference
        inputHandler.handleInput(inputOf(bytes))

        // then
        expectThat(givenEgg.viewEggId()) isEqualTo reference.slice(1)
        expectThat(bytes) isNotEqualTo reference
    }
}
