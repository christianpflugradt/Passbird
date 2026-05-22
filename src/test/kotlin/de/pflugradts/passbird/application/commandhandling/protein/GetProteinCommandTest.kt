package de.pflugradts.passbird.application.commandhandling.protein

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.createInputHandlerFor
import de.pflugradts.passbird.application.commandhandling.handler.protein.GetProteinCommandHandler
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.S4
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.Called
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

@Tag(INTEGRATION)
class GetProteinCommandTest {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val clipboardAdapterPort = mockk<ClipboardAdapterPort>(relaxed = true)
    private val passwordService = mockk<PasswordService>()
    private val getProteinCommandHandler = GetProteinCommandHandler(passwordService, clipboardAdapterPort, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(getProteinCommandHandler)

    @ParameterizedTest
    @EnumSource(value = Slot::class)
    fun `should handle get protein command`(slot: Slot) {
        // given
        val args = "EggId"
        val command = shellOf("p${slot.index()}$args")
        val reference = command.copy()
        val expectedStructure = shellOf("username")
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(
                createEggForTesting(
                    withEggIdShell = shellOf(args),
                    withProteins = mapOf(slot to Pair(shellOf("type"), expectedStructure)),
                ),
            ),
        )
        val outputSlot = slot<Output>()

        // when
        expectThat(command) isEqualTo reference
        inputHandler.handleInput(inputOf(command))

        // then
        verify { clipboardAdapterPort.post(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell) isEqualTo expectedStructure
        verify(exactly = 1) { userInterfaceAdapterPort.send(any()) }
        expectThat(command) isNotEqualTo reference
    }

    @Test
    fun `should handle get protein command with non existing egg`() {
        // given
        val args = "EggId"
        val command = shellOf("p0$args")
        val reference = command.copy()
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(createEggForTesting(withEggIdShell = shellOf("other"))),
        )

        // when
        expectThat(command) isEqualTo reference
        inputHandler.handleInput(inputOf(command))

        // then
        verify { clipboardAdapterPort wasNot Called }
        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
        expectThat(command) isNotEqualTo reference
    }

    @Test
    fun `should handle get protein command with non-existing protein`() {
        // given
        val args = "EggId"
        val command = shellOf("p3$args")
        val otherSlot = S4
        val reference = command.copy()
        val expectedStructure = shellOf("username")
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(
                createEggForTesting(
                    withEggIdShell = shellOf(args),
                    withProteins = mapOf(otherSlot to Pair(shellOf("type"), expectedStructure)),
                ),
            ),
        )

        // when
        expectThat(command) isEqualTo reference
        inputHandler.handleInput(inputOf(command))

        // then
        verify { clipboardAdapterPort wasNot Called }
        verify(exactly = 1) {
            userInterfaceAdapterPort.send(eq(outputOf(shellOf("Specified Protein Structure is empty - Operation aborted."))))
        }
        expectThat(command) isNotEqualTo reference
    }
}
