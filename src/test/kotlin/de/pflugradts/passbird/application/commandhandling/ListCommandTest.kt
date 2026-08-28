package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.kotlinextensions.CapturedOutputPrintStream.Companion.captureSystemErr
import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.ListCommandHandler
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.slotAt
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.slot.Slot.S2
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.HIGHLIGHT
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.SPECIAL
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.nest.createNestServiceForTesting
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.DEFAULT as DEFAULT_FORMATTING

@Tag(INTEGRATION)
internal class ListCommandTest {

    private val userInterfaceAdapterPort = RecordingUserInterfaceAdapterPort()
    private val passwordService = mockk<PasswordService>()
    private val nestService = createNestServiceForTesting()
    private val listCommandHandler = ListCommandHandler(nestService, passwordService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(listCommandHandler)

    @Test
    fun `should handle list command`() {
        // given
        val input = inputOf(shellOf("l"))
        val eggId1 = shellOf("EggId1")
        val eggId2 = shellOf("EggId2")
        val eggId3 = shellOf("EggId3")
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(
                createEggForTesting(withEggIdShell = eggId1),
                createEggForTesting(withEggIdShell = eggId2),
                createEggForTesting(withEggIdShell = eggId3),
            ),
            withNestService = nestService,
        )

        // when
        inputHandler.handleInput(input)

        // then
        expectThat(userInterfaceAdapterPort.sentOutputs.single().renderedText()) isEqualTo
            "${eggId1.asString()}, ${eggId2.asString()}, ${eggId3.asString()}"
    }

    @Test
    fun `should scramble listed eggId shells after rendering current nest`() {
        // given
        val input = inputOf(shellOf("l"))
        val eggId1 = spyk(shellOf("EggId1"))
        val eggId2 = spyk(shellOf("EggId2"))
        every { passwordService.findAllEggIds() } returns listOf(eggId1, eggId2).stream()

        // when
        inputHandler.handleInput(input)

        // then
        expectThat(userInterfaceAdapterPort.sentOutputs.single().renderedText()) isEqualTo "EggId1, EggId2"
        verify(exactly = 1) { eggId1.scramble() }
        verify(exactly = 1) { eggId2.scramble() }
        expectThat(eggId1) isNotEqualTo shellOf("EggId1")
        expectThat(eggId2) isNotEqualTo shellOf("EggId2")
    }

    @Test
    fun `should handle list command with case insensitive filter`() {
        // given
        val input = inputOf(shellOf("lmiro"))
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(
                createEggForTesting(withEggIdShell = shellOf("Miro")),
                createEggForTesting(withEggIdShell = shellOf("Mail")),
                createEggForTesting(withEggIdShell = shellOf("miroBoard")),
                createEggForTesting(withEggIdShell = shellOf("miroWork"), withSlot = slotAt(2)),
            ),
            withNestService = nestService,
        )
        nestService.place(shellOf("Work"), slotAt(2))

        // when
        inputHandler.handleInput(input)

        // then
        expectThat(userInterfaceAdapterPort.sentOutputs.single().renderedText()) isEqualTo "Miro, miroBoard"
        expectThat(userInterfaceAdapterPort.sentOutputs.single().textAndFormatting()) containsExactly listOf(
            "Miro" to SPECIAL,
            ", " to DEFAULT_FORMATTING,
            "miro" to SPECIAL,
            "Board" to DEFAULT_FORMATTING,
        )
    }

    @Test
    fun `should handle global list command grouped by nest`() {
        // given
        val input = inputOf(shellOf("l*"))
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(
                createEggForTesting(withEggIdShell = shellOf("EggId1")),
                createEggForTesting(withEggIdShell = shellOf("EggId2")),
                createEggForTesting(withEggIdShell = shellOf("EggId3"), withSlot = slotAt(2)),
            ),
            withNestService = nestService,
        )
        nestService.place(shellOf("Work"), slotAt(2))

        // when
        inputHandler.handleInput(input)

        // then
        expectThat(userInterfaceAdapterPort.sentOutputs.single().renderedText()) isEqualTo
            "0: Default\n\tEggId1, EggId2\n2: Work\n\tEggId3"
        expectThat(userInterfaceAdapterPort.sentOutputs.single().textAndFormatting()) containsExactly listOf(
            "0: Default" to HIGHLIGHT,
            "\n\t" to DEFAULT_FORMATTING,
            "EggId1" to DEFAULT_FORMATTING,
            ", " to DEFAULT_FORMATTING,
            "EggId2" to DEFAULT_FORMATTING,
            "\n" to DEFAULT_FORMATTING,
            "2: Work" to HIGHLIGHT,
            "\n\t" to DEFAULT_FORMATTING,
            "EggId3" to DEFAULT_FORMATTING,
        )
    }

    @Test
    fun `should scramble listed eggId shells after rendering all nests`() {
        // given
        val input = inputOf(shellOf("l*"))
        val defaultEggId = spyk(shellOf("DefaultEgg"))
        val nestedEggId = spyk(shellOf("NestedEgg"))
        nestService.place(shellOf("Work"), S2)
        every { passwordService.findAllEggIds(DEFAULT) } returns listOf(defaultEggId).stream()
        every { passwordService.findAllEggIds(S2) } returns listOf(nestedEggId).stream()

        // when
        inputHandler.handleInput(input)

        // then
        expectThat(userInterfaceAdapterPort.sentOutputs.single().renderedText()) isEqualTo
            "0: Default\n\tDefaultEgg\n2: Work\n\tNestedEgg"
        verify(exactly = 1) { defaultEggId.scramble() }
        verify(exactly = 1) { nestedEggId.scramble() }
        expectThat(defaultEggId) isNotEqualTo shellOf("DefaultEgg")
        expectThat(nestedEggId) isNotEqualTo shellOf("NestedEgg")
    }

    @Test
    fun `should handle global list command with case insensitive filter grouped by nest`() {
        // given
        val input = inputOf(shellOf("l*miro"))
        fakePasswordService(
            instance = passwordService,
            withEggs = listOf(
                createEggForTesting(withEggIdShell = shellOf("Miro")),
                createEggForTesting(withEggIdShell = shellOf("Mail")),
                createEggForTesting(withEggIdShell = shellOf("miroWork"), withSlot = slotAt(2)),
                createEggForTesting(withEggIdShell = shellOf("Calendar"), withSlot = slotAt(3)),
            ),
            withNestService = nestService,
        )
        nestService.place(shellOf("Work"), slotAt(2))
        nestService.place(shellOf("Private"), slotAt(3))

        // when
        inputHandler.handleInput(input)

        // then
        expectThat(userInterfaceAdapterPort.sentOutputs.single().renderedText()) isEqualTo "0: Default\n\tMiro\n2: Work\n\tmiroWork"
        expectThat(userInterfaceAdapterPort.sentOutputs.single().textAndFormatting()) containsExactly listOf(
            "0: Default" to HIGHLIGHT,
            "\n\t" to DEFAULT_FORMATTING,
            "Miro" to SPECIAL,
            "\n" to DEFAULT_FORMATTING,
            "2: Work" to HIGHLIGHT,
            "\n\t" to DEFAULT_FORMATTING,
            "miro" to SPECIAL,
            "Work" to DEFAULT_FORMATTING,
        )
    }

    @Test
    fun `should handle list command with empty nest`() {
        // given
        val input = inputOf(shellOf("l"))
        fakePasswordService(instance = passwordService, withEggs = emptyList())

        // when
        inputHandler.handleInput(input)

        // then
        expectThat(userInterfaceAdapterPort.sentOutputs.single().renderedText()) isEqualTo "Nest is empty"
    }

    @Test
    fun `should reject list command with unsupported command variant`() {
        // given
        val captureSystemErr = captureSystemErr()

        // when
        captureSystemErr.during {
            inputHandler.handleInput(inputOf(shellOf("l?")))
        }

        // then
        verify(exactly = 0) { passwordService.findAllEggIds() }
        expectThat(userInterfaceAdapterPort.sentOutputs).isEmpty()
        expectThat(captureSystemErr.capture) isEqualTo ""
    }

    private fun List<Output>.renderedText() = joinToString(separator = "") { it.shell.asString() }
    private fun List<Output>.textAndFormatting() = map { it.shell.asString() to it.formatting }

    private class RecordingUserInterfaceAdapterPort : UserInterfaceAdapterPort {
        val sentOutputs = mutableListOf<List<Output>>()

        override fun receive(vararg output: Output) = error("not used in ListCommandTest")
        override fun receiveSecurely(output: Output) = error("not used in ListCommandTest")
        override fun receiveLineBreakWithin(milliseconds: Long) = error("not used in ListCommandTest")
        override fun send(vararg output: Output) {
            output.toList().takeUnless { outputs -> outputs.all { it.shell.isEmpty } }?.let(sentOutputs::add)
        }
        override fun startEphemeralLine(output: Output) = error("not used in ListCommandTest")
        override fun updateEphemeralLine(output: Output) = error("not used in ListCommandTest")
        override fun finishEphemeralLine() = error("not used in ListCommandTest")
        override fun warningSound() = error("not used in ListCommandTest")
    }
}
