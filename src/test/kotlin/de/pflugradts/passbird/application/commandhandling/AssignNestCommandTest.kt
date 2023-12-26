package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.nest.AssignNestCommandHandler
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.egg.createEggForTesting
import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.at
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.createNestServiceForTesting
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue

@Tag(INTEGRATION)
class AssignNestCommandTest {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val nestService = createNestServiceForTesting()
    private val passwordService = mockk<PasswordService>()
    private val assignNestCommandHandler = AssignNestCommandHandler(nestService, passwordService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(assignNestCommandHandler)

    @Test
    fun `should handle assign nest command`() {
        // given
        val givenEggId = "a"
        val givenInput = shellOf("n$givenEggId")
        val referenceInput = givenInput.copy()
        val expectedNestSlot = 1
        val givenEgg = createEggForTesting(withEggIdShell = shellOf(givenEggId))
        nestService.place(shellOf("Nest"), at(expectedNestSlot))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(expectedNestSlot)))

        // when
        expectThat(givenInput) isEqualTo referenceInput
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify(exactly = 1) { passwordService.moveEgg(shellOf(givenEggId), at(expectedNestSlot)) }
        expectThat(givenInput) isNotEqualTo referenceInput
    }

    @Test
    fun `should handle egg not exists`() {
        // given
        val givenEggId = "a"
        val givenInput = shellOf("n$givenEggId")
        val referenceInput = givenInput.copy()
        val expectedNestSlot = 1
        nestService.place(shellOf("Nest"), at(expectedNestSlot))
        fakePasswordService(instance = passwordService)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(expectedNestSlot)))

        // when
        expectThat(givenInput) isEqualTo referenceInput
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify(exactly = 0) { passwordService.moveEgg(any(), any()) }
        expectThat(givenInput) isNotEqualTo referenceInput
    }

    @Test
    fun `should display target nest`() {
        // given
        val givenEggId = "a"
        val givenInput = shellOf("n$givenEggId")
        val referenceInput = givenInput.copy()
        val givenEgg = createEggForTesting(shellOf(givenEggId))
        val currentNestSlot = 0
        val targetNestSlot = 1
        nestService.moveToNestAt(at(currentNestSlot))
        nestService.place(shellOf("Nest"), at(targetNestSlot))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(targetNestSlot)))
        val outputSlot = slot<Output>()

        // when
        expectThat(givenInput) isEqualTo referenceInput
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell.asString()).contains("$targetNestSlot: ")
        expectThat(outputSlot.captured.shell.asString()).not().contains("$currentNestSlot: ")
        expectThat(givenInput) isNotEqualTo referenceInput
    }

    @Test
    fun `should display target nest when not default`() {
        // given
        val givenEggId = "a"
        val givenInput = shellOf("n$givenEggId")
        val referenceInput = givenInput.copy()
        val currentNestSlot = 1
        val targetNestSlot = 0
        val givenEgg = createEggForTesting(withEggIdShell = shellOf(givenEggId), withNestSlot = at(currentNestSlot))
        nestService.place(shellOf("Nest"), at(currentNestSlot))
        nestService.moveToNestAt(at(currentNestSlot))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(targetNestSlot)))
        val outputSlot = slot<Output>()

        // when
        expectThat(givenInput) isEqualTo referenceInput
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.shell.asString()).contains("$targetNestSlot: ")
        expectThat(outputSlot.captured.shell.asString()).not().contains("$currentNestSlot: ")
        expectThat(givenInput) isNotEqualTo referenceInput
    }

    @Test
    fun `should handle invalid nest entered`() {
        // given
        val givenEggId = "a"
        val givenInput = shellOf("n$givenEggId")
        val referenceInput = givenInput.copy()
        val givenEgg = createEggForTesting(withEggIdShell = shellOf(givenEggId))
        val invalidNestSlot = -1
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(invalidNestSlot)))
        val outputSlot = mutableListOf<Output>()

        // when
        expectThat(givenInput) isEqualTo referenceInput
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot) hasSize 2
        expectThat(outputSlot[1].shell.asString()) contains "Invalid Nest"
        verify(exactly = 0) { passwordService.moveEgg(any(), any()) }
        expectThat(givenInput) isNotEqualTo referenceInput
    }

    @Test
    fun `should handle current nest entered`() {
        // given
        val givenEggId = "a"
        val givenInput = shellOf("n$givenEggId")
        val referenceInput = givenInput.copy()
        val givenEgg = createEggForTesting(withEggIdShell = shellOf(givenEggId))
        val currentNestSlot = 1
        nestService.place(shellOf("Nest"), at(currentNestSlot))
        nestService.moveToNestAt(at(currentNestSlot))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(currentNestSlot)))
        val outputSlot = mutableListOf<Output>()

        // when
        expectThat(givenInput) isEqualTo referenceInput
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot) hasSize 2
        expectThat(outputSlot[1].shell.asString()) contains "Egg is already in the specified Nest"
        verify(exactly = 0) { passwordService.moveEgg(any(), any()) }
        expectThat(givenInput) isNotEqualTo referenceInput
    }

    @Test
    fun `should handle empty nest entered`() {
        // given
        val givenEggId = "a"
        val givenInput = shellOf("n$givenEggId")
        val referenceInput = givenInput.copy()
        val givenEgg = createEggForTesting(withEggIdShell = shellOf(givenEggId))
        val targetNestSlot = 1
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(targetNestSlot)))
        expectThat(nestService.atNestSlot(at(targetNestSlot)).isEmpty).isTrue()
        val outputSlot = mutableListOf<Output>()

        // when
        expectThat(givenInput) isEqualTo referenceInput
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot) hasSize 2
        expectThat(outputSlot[1].shell.asString()) contains "Specified Nest does not exist"
        verify(exactly = 0) { passwordService.moveEgg(any(), any()) }
        expectThat(givenInput) isNotEqualTo referenceInput
    }

    @Test
    fun `should handle other password with same eggId in target nest`() {
        // given
        val givenEggId = "a"
        val givenInput = shellOf("n$givenEggId")
        val referenceInput = givenInput.copy()
        val currentNestSlot = 0
        val targetNestSlot = 1
        val givenEgg1 = createEggForTesting(withEggIdShell = shellOf(givenEggId), withNestSlot = at(currentNestSlot))
        val givenEgg2 = createEggForTesting(withEggIdShell = shellOf(givenEggId), withNestSlot = at(targetNestSlot))
        nestService.moveToNestAt(at(currentNestSlot))
        nestService.place(shellOf("Nest"), at(targetNestSlot))
        fakePasswordService(instance = passwordService, withEggs = listOf(givenEgg1, givenEgg2))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(targetNestSlot)))
        val outputSlot = mutableListOf<Output>()

        // when
        expectThat(givenInput) isEqualTo referenceInput
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot) hasSize 2
        expectThat(outputSlot[1].shell.asString()) contains "Egg with same EggId already exists in target Nest"
        verify(exactly = 0) { passwordService.moveEgg(any(), any()) }
        expectThat(givenInput) isNotEqualTo referenceInput
    }

    private fun inputOf(index: Int) = inputOf(shellOf(index.toString()))
}
