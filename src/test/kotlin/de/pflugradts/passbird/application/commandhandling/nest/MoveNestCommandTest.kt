package de.pflugradts.passbird.application.commandhandling.nest

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.createInputHandlerFor
import de.pflugradts.passbird.application.commandhandling.handler.nest.MoveNestCommandHandler
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.nest.createNestServiceForTesting
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

@Tag(INTEGRATION)
class MoveNestCommandTest {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val nestService = createNestServiceForTesting()
    private val commandExecutionTracker = CommandExecutionTracker()
    private val moveNestCommandHandler = MoveNestCommandHandler(nestService, userInterfaceAdapterPort, commandExecutionTracker)
    private val inputHandler = createInputHandlerFor(moveNestCommandHandler, commandExecutionTracker)

    @Test
    fun `should move nest to free slot`() {
        // given
        nestService.place(shellOf("Nest"), Slot.S2)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(shellOf("5"))))

        // when
        inputHandler.handleInput(inputOf(shellOf("n>2")))

        // then
        expectThat(nestService.atNestSlot(Slot.S2).isEmpty).isTrue()
        expectThat(nestService.atNestSlot(Slot.S5).get().viewNestId().asString()) isEqualTo "Nest"
    }

    @Test
    fun `should list only free custom slots`() {
        // given
        nestService.place(shellOf("source"), Slot.S2)
        nestService.place(shellOf("occupied"), Slot.S4)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(shellOf("5"))))
        val outputs = mutableListOf<Output>()

        // when
        inputHandler.handleInput(inputOf(shellOf("n>2")))

        // then
        verify { userInterfaceAdapterPort.send(capture(outputs)) }
        expectThat(outputs) hasSize 1
        expectThat(outputs.single().shell.asString()) contains "Available free Nest Slots: 1, 3, 5, 6, 7, 8, 9"
    }

    @Test
    fun `should abort moving default nest`() {
        // given
        val outputs = mutableListOf<Output>()

        // when
        inputHandler.handleInput(inputOf(shellOf("n>0")))

        // then
        verify { userInterfaceAdapterPort.send(capture(outputs)) }
        expectThat(outputs.single().shell.asString()) isEqualTo "Default Nest cannot be moved - Operation aborted."
    }

    @Test
    fun `should abort moving missing nest`() {
        // given
        val outputs = mutableListOf<Output>()

        // when
        inputHandler.handleInput(inputOf(shellOf("n>2")))

        // then
        verify { userInterfaceAdapterPort.send(capture(outputs)) }
        expectThat(outputs.single().shell.asString()) isEqualTo "Specified Nest does not exist - Operation aborted."
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "x", "10"])
    fun `should abort moving nest when target input is invalid`(invalidTargetSlot: String) {
        // given
        nestService.place(shellOf("Nest"), Slot.S2)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(shellOf(invalidTargetSlot))))
        val outputs = mutableListOf<Output>()

        // when
        inputHandler.handleInput(inputOf(shellOf("n>2")))

        // then
        verify { userInterfaceAdapterPort.send(capture(outputs)) }
        expectThat(outputs) hasSize 2
        expectThat(outputs[1].shell.asString()) isEqualTo "Operation aborted."
        expectThat(nestService.atNestSlot(Slot.S2).get().viewNestId().asString()) isEqualTo "Nest"
    }

    @Test
    fun `should abort moving nest when target slot is not free`() {
        // given
        nestService.place(shellOf("Nest"), Slot.S2)
        nestService.place(shellOf("occupied"), Slot.S5)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(shellOf("5"))))
        val outputs = mutableListOf<Output>()

        // when
        inputHandler.handleInput(inputOf(shellOf("n>2")))

        // then
        verify { userInterfaceAdapterPort.send(capture(outputs)) }
        expectThat(outputs) hasSize 2
        expectThat(outputs[1].shell.asString()) isEqualTo "Specified Nest Slot is not free - Operation aborted."
        expectThat(nestService.atNestSlot(Slot.S2).get().viewNestId().asString()) isEqualTo "Nest"
    }

    @Test
    fun `should keep moved nest current`() {
        // given
        nestService.place(shellOf("Nest"), Slot.S2)
        nestService.moveToNestAt(Slot.S2)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(shellOf("5"))))

        // when
        inputHandler.handleInput(inputOf(shellOf("n>2")))

        // then
        expectThat(nestService.currentNest().slot) isEqualTo Slot.S5
    }
}
