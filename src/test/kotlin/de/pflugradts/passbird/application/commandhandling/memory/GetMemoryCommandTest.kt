package de.pflugradts.passbird.application.commandhandling.memory

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.createInputHandlerFor
import de.pflugradts.passbird.application.commandhandling.handler.memory.GetMemoryCommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.slot.Slot.S1
import de.pflugradts.passbird.domain.model.slot.Slot.S2
import de.pflugradts.passbird.domain.model.slot.Slot.S3
import de.pflugradts.passbird.domain.model.slot.Slot.S4
import de.pflugradts.passbird.domain.model.slot.Slot.S5
import de.pflugradts.passbird.domain.model.slot.Slot.S6
import de.pflugradts.passbird.domain.model.slot.Slot.S7
import de.pflugradts.passbird.domain.model.slot.Slot.S8
import de.pflugradts.passbird.domain.model.slot.Slot.S9
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.stream.Stream

@Tag(INTEGRATION)
class GetMemoryCommandTest {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val clipboardAdapterPort = mockk<ClipboardAdapterPort>(relaxed = true)
    private val passwordService = mockk<PasswordService>()
    private val getMemoryCommandHandler = GetMemoryCommandHandler(passwordService, clipboardAdapterPort, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(getMemoryCommandHandler)

    @ParameterizedTest
    @MethodSource("providedMemory")
    fun `should handle get memory command`(slot: Slot, eggId: String) {
        // given
        val command = shellOf("m${slot.index()}")
        fakePasswordService(
            instance = passwordService,
            withMemory = testMemoryData(),
        )
        val outputSlot = slot<Output>()

        // when
        inputHandler.handleInput(inputOf(command))

        // then
        verify { clipboardAdapterPort.post(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell) isEqualTo shellOf(testMemoryData()[slot].orEmpty())
        verify(exactly = 1) { userInterfaceAdapterPort.send(outputOf(shellOf("EggId copied to clipboard."))) }
    }

    @ParameterizedTest
    @EnumSource(Slot::class)
    fun `should handle get memory command on empty memory slot`(slot: Slot) {
        // given
        val command = shellOf("m${slot.index()}")
        fakePasswordService(
            instance = passwordService,
            withMemory = emptyMap(),
        )

        // when
        inputHandler.handleInput(inputOf(command))

        // then
        verify { clipboardAdapterPort wasNot Called }
        verify(exactly = 1) { userInterfaceAdapterPort.send(outputOf(shellOf("Memory entry at slot ${slot.index()} does not exist."))) }
    }

    companion object {
        @JvmStatic
        private fun providedMemory(): Stream<Arguments> = testMemoryData().map { Arguments.of(it.key, it.value) }.stream()
    }
}

fun testMemoryData() = mapOf(
    DEFAULT to "a",
    S1 to "b",
    S2 to "c",
    S3 to "d",
    S4 to "e",
    S5 to "f",
    S6 to "g",
    S7 to "h",
    S8 to "i",
    S9 to "j",
)
