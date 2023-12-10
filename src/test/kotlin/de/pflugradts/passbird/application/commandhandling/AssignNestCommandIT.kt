package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.nest.AssignNestCommandHandler
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.nest.Slot.Companion.at
import de.pflugradts.passbird.domain.model.password.createPasswordEntryForTesting
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.createNestServiceForTesting
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue

class AssignNestCommandIT {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val nestService = createNestServiceForTesting()
    private val passwordService = mockk<PasswordService>()
    private val assignNestCommandHandler = AssignNestCommandHandler(nestService, passwordService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(assignNestCommandHandler)

    @Test
    fun `should handle assign nest command`() {
        // given
        val givenAlias = "a"
        val givenInput = bytesOf("n$givenAlias")
        val referenceInput = givenInput.copy()
        val expectedNestSlot = 1
        val givenPasswordEntry = createPasswordEntryForTesting(withKeyBytes = bytesOf(givenAlias))
        nestService.deploy(bytesOf("nest"), at(expectedNestSlot))
        fakePasswordService(instance = passwordService, withPasswordEntries = listOf(givenPasswordEntry))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(expectedNestSlot)))

        // when
        expectThat(givenInput) isEqualTo referenceInput
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify(exactly = 1) { passwordService.movePasswordEntry(bytesOf(givenAlias), at(expectedNestSlot)) }
        expectThat(givenInput) isNotEqualTo referenceInput
    }

    @Test
    fun `should handle entry not exists`() {
        // given
        val givenAlias = "a"
        val givenInput = bytesOf("n$givenAlias")
        val referenceInput = givenInput.copy()
        val expectedNestSlot = 1
        nestService.deploy(bytesOf("nest"), at(expectedNestSlot))
        fakePasswordService(instance = passwordService)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(expectedNestSlot)))

        // when
        expectThat(givenInput) isEqualTo referenceInput
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify(exactly = 0) { passwordService.movePasswordEntry(any(), any()) }
        expectThat(givenInput) isNotEqualTo referenceInput
    }

    @Test
    fun `should display target nest`() {
        // given
        val givenAlias = "a"
        val givenInput = bytesOf("n$givenAlias")
        val referenceInput = givenInput.copy()
        val givenPasswordEntry = createPasswordEntryForTesting(bytesOf(givenAlias))
        val currentNestSlot = 0
        val targetNestSlot = 1
        nestService.moveToNestAt(at(currentNestSlot))
        nestService.deploy(bytesOf("nest"), at(targetNestSlot))
        fakePasswordService(instance = passwordService, withPasswordEntries = listOf(givenPasswordEntry))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(targetNestSlot)))
        val outputSlot = slot<Output>()

        // when
        expectThat(givenInput) isEqualTo referenceInput
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.bytes.asString()).contains("$targetNestSlot: ")
        expectThat(outputSlot.captured.bytes.asString()).not().contains("$currentNestSlot: ")
        expectThat(givenInput) isNotEqualTo referenceInput
    }

    @Test
    fun `should display target nest when not default`() {
        // given
        val givenAlias = "a"
        val givenInput = bytesOf("n$givenAlias")
        val referenceInput = givenInput.copy()
        val currentNestSlot = 1
        val targetNestSlot = 0
        val givenPasswordEntry = createPasswordEntryForTesting(withKeyBytes = bytesOf(givenAlias), withNestSlot = at(currentNestSlot))
        nestService.deploy(bytesOf("nest"), at(currentNestSlot))
        nestService.moveToNestAt(at(currentNestSlot))
        fakePasswordService(instance = passwordService, withPasswordEntries = listOf(givenPasswordEntry))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(targetNestSlot)))
        val outputSlot = slot<Output>()

        // when
        expectThat(givenInput) isEqualTo referenceInput
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.bytes.asString()).contains("$targetNestSlot: ")
        expectThat(outputSlot.captured.bytes.asString()).not().contains("$currentNestSlot: ")
        expectThat(givenInput) isNotEqualTo referenceInput
    }

    @Test
    fun `should handle invalid nest entered`() {
        // given
        val givenAlias = "a"
        val givenInput = bytesOf("n$givenAlias")
        val referenceInput = givenInput.copy()
        val givenPasswordEntry = createPasswordEntryForTesting(withKeyBytes = bytesOf(givenAlias))
        val invalidNestSlot = -1
        fakePasswordService(instance = passwordService, withPasswordEntries = listOf(givenPasswordEntry))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(invalidNestSlot)))
        val outputSlot = mutableListOf<Output>()

        // when
        expectThat(givenInput) isEqualTo referenceInput
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot) hasSize 2
        expectThat(outputSlot[1].bytes.asString()) contains "Invalid namespace"
        verify(exactly = 0) { passwordService.movePasswordEntry(any(), any()) }
        expectThat(givenInput) isNotEqualTo referenceInput
    }

    @Test
    fun `should handle current nest entered`() {
        // given
        val givenAlias = "a"
        val givenInput = bytesOf("n$givenAlias")
        val referenceInput = givenInput.copy()
        val givenPasswordEntry = createPasswordEntryForTesting(withKeyBytes = bytesOf(givenAlias))
        val currentNestSlot = 1
        nestService.deploy(bytesOf("nest"), at(currentNestSlot))
        nestService.moveToNestAt(at(currentNestSlot))
        fakePasswordService(instance = passwordService, withPasswordEntries = listOf(givenPasswordEntry))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(currentNestSlot)))
        val outputSlot = mutableListOf<Output>()

        // when
        expectThat(givenInput) isEqualTo referenceInput
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot) hasSize 2
        expectThat(outputSlot[1].bytes.asString()) contains "Password entry is already in the specified namespace"
        verify(exactly = 0) { passwordService.movePasswordEntry(any(), any()) }
        expectThat(givenInput) isNotEqualTo referenceInput
    }

    @Test
    fun `should handle empty nest entered`() {
        // given
        val givenAlias = "a"
        val givenInput = bytesOf("n$givenAlias")
        val referenceInput = givenInput.copy()
        val givenPasswordEntry = createPasswordEntryForTesting(withKeyBytes = bytesOf(givenAlias))
        val targetNestSlot = 1
        fakePasswordService(instance = passwordService, withPasswordEntries = listOf(givenPasswordEntry))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(targetNestSlot)))
        expectThat(nestService.atSlot(at(targetNestSlot)).isEmpty).isTrue()
        val outputSlot = mutableListOf<Output>()

        // when
        expectThat(givenInput) isEqualTo referenceInput
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot) hasSize 2
        expectThat(outputSlot[1].bytes.asString()) contains "Specified namespace does not exist"
        verify(exactly = 0) { passwordService.movePasswordEntry(any(), any()) }
        expectThat(givenInput) isNotEqualTo referenceInput
    }

    @Test
    fun `should handle other password with same alias in target nest`() {
        // given
        val givenAlias = "a"
        val givenInput = bytesOf("n$givenAlias")
        val referenceInput = givenInput.copy()
        val currentNestSlot = 0
        val targetNestSlot = 1
        val givenPasswordEntry1 = createPasswordEntryForTesting(withKeyBytes = bytesOf(givenAlias), withNestSlot = at(currentNestSlot))
        val givenPasswordEntry2 = createPasswordEntryForTesting(withKeyBytes = bytesOf(givenAlias), withNestSlot = at(targetNestSlot))
        nestService.moveToNestAt(at(currentNestSlot))
        nestService.deploy(bytesOf("nest"), at(targetNestSlot))
        fakePasswordService(instance = passwordService, withPasswordEntries = listOf(givenPasswordEntry1, givenPasswordEntry2))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(targetNestSlot)))
        val outputSlot = mutableListOf<Output>()

        // when
        expectThat(givenInput) isEqualTo referenceInput
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot) hasSize 2
        expectThat(outputSlot[1].bytes.asString()) contains "Password entry with same alias already exists in target namespace"
        verify(exactly = 0) { passwordService.movePasswordEntry(any(), any()) }
        expectThat(givenInput) isNotEqualTo referenceInput
    }

    private fun inputOf(index: Int) = inputOf(bytesOf(index.toString()))
}
