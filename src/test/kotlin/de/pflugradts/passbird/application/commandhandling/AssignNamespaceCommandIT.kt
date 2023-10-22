package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.namespace.AssignNamespaceCommandHandler
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.Companion.at
import de.pflugradts.passbird.domain.model.password.createPasswordEntryForTesting
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.createNamespaceServiceForTesting
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

class AssignNamespaceCommandIT {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val namespaceService = createNamespaceServiceForTesting()
    private val passwordService = mockk<PasswordService>()
    private val assignNamespaceCommandHandler = AssignNamespaceCommandHandler(namespaceService, passwordService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(assignNamespaceCommandHandler)

    @Test
    fun shouldHandleAssignNamespaceCommand() {
        // given
        val givenAlias = "a"
        val givenInput = bytesOf("n$givenAlias")
        val referenceInput = givenInput.copy()
        val expectedNamespace = 1
        val givenPasswordEntry = createPasswordEntryForTesting(withKeyBytes = bytesOf(givenAlias))
        namespaceService.deploy(bytesOf("namespace"), at(expectedNamespace))
        fakePasswordService(instance = passwordService, withPasswordEntries = listOf(givenPasswordEntry))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(expectedNamespace)))

        // when
        expectThat(givenInput) isEqualTo referenceInput
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify(exactly = 1) { passwordService.movePasswordEntry(bytesOf(givenAlias), at(expectedNamespace)) }
        expectThat(givenInput) isNotEqualTo referenceInput
    }

    @Test
    fun shouldHandleAssignNamespaceCommand_EntryNotExists() {
        // given
        val givenAlias = "a"
        val givenInput = bytesOf("n$givenAlias")
        val referenceInput = givenInput.copy()
        val expectedNamespace = 1
        namespaceService.deploy(bytesOf("namespace"), at(expectedNamespace))
        fakePasswordService(instance = passwordService)
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(expectedNamespace)))

        // when
        expectThat(givenInput) isEqualTo referenceInput
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify(exactly = 0) { passwordService.movePasswordEntry(any(), any()) }
        expectThat(givenInput) isNotEqualTo referenceInput
    }

    @Test
    fun shouldHandleAssignNamespaceCommand_DisplayTargetNamespace() {
        // given
        val givenAlias = "a"
        val givenInput = bytesOf("n$givenAlias")
        val referenceInput = givenInput.copy()
        val givenPasswordEntry = createPasswordEntryForTesting(bytesOf(givenAlias))
        val currentNamespace = 0
        val targetNamespace = 1
        namespaceService.updateCurrentNamespace(at(currentNamespace))
        namespaceService.deploy(bytesOf("namespace"), at(targetNamespace))
        fakePasswordService(instance = passwordService, withPasswordEntries = listOf(givenPasswordEntry))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(targetNamespace)))
        val outputSlot = slot<Output>()

        // when
        expectThat(givenInput) isEqualTo referenceInput
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.isCaptured).isTrue()
        expectThat(outputSlot.captured.bytes.asString()).contains("$targetNamespace: ")
        expectThat(outputSlot.captured.bytes.asString()).not().contains("$currentNamespace: ")
        expectThat(givenInput) isNotEqualTo referenceInput
    }

    @Test
    fun shouldHandleAssignNamespaceCommand_DisplayTargetNamespaceWhenNotDefault() {
        // given
        val givenAlias = "a"
        val givenInput = bytesOf("n$givenAlias")
        val referenceInput = givenInput.copy()
        val currentNamespace = 1
        val targetNamespace = 0
        val givenPasswordEntry = createPasswordEntryForTesting(withKeyBytes = bytesOf(givenAlias), withNamespace = at(currentNamespace))
        namespaceService.deploy(bytesOf("namespace"), at(currentNamespace))
        namespaceService.updateCurrentNamespace(at(currentNamespace))
        fakePasswordService(instance = passwordService, withPasswordEntries = listOf(givenPasswordEntry))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(targetNamespace)))
        val outputSlot = slot<Output>()

        // when
        expectThat(givenInput) isEqualTo referenceInput
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.isCaptured).isTrue()
        expectThat(outputSlot.captured.bytes.asString()).contains("$targetNamespace: ")
        expectThat(outputSlot.captured.bytes.asString()).not().contains("$currentNamespace: ")
        expectThat(givenInput) isNotEqualTo referenceInput
    }

    @Test
    fun shouldHandleAssignNamespaceCommand_EnteredInvalidNamespace() {
        // given
        val givenAlias = "a"
        val givenInput = bytesOf("n$givenAlias")
        val referenceInput = givenInput.copy()
        val givenPasswordEntry = createPasswordEntryForTesting(withKeyBytes = bytesOf(givenAlias))
        val invalidNamespace = -1
        fakePasswordService(instance = passwordService, withPasswordEntries = listOf(givenPasswordEntry))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(invalidNamespace)))
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
    fun shouldHandleAssignNamespaceCommand_EnteredCurrentNamespace() {
        // given
        val givenAlias = "a"
        val givenInput = bytesOf("n$givenAlias")
        val referenceInput = givenInput.copy()
        val givenPasswordEntry = createPasswordEntryForTesting(withKeyBytes = bytesOf(givenAlias))
        val currentNamespace = 1
        namespaceService.deploy(bytesOf("namespace"), at(currentNamespace))
        namespaceService.updateCurrentNamespace(at(currentNamespace))
        fakePasswordService(instance = passwordService, withPasswordEntries = listOf(givenPasswordEntry))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(currentNamespace)))
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
    fun shouldHandleAssignNamespaceCommand_EnteredEmptyNamespace() {
        // given
        val givenAlias = "a"
        val givenInput = bytesOf("n$givenAlias")
        val referenceInput = givenInput.copy()
        val givenPasswordEntry = createPasswordEntryForTesting(withKeyBytes = bytesOf(givenAlias))
        val targetNamespace = 1
        fakePasswordService(instance = passwordService, withPasswordEntries = listOf(givenPasswordEntry))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(targetNamespace)))
        expectThat(namespaceService.atSlot(at(targetNamespace)).isEmpty).isTrue()
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
    fun shouldHandleAssignNamespaceCommand_OtherPasswordWithSameAliasAlreadyInTargetNamespace() {
        // given
        val givenAlias = "a"
        val givenInput = bytesOf("n$givenAlias")
        val referenceInput = givenInput.copy()
        val currentNamespace = 0
        val targetNamespace = 1
        val givenPasswordEntry1 = createPasswordEntryForTesting(withKeyBytes = bytesOf(givenAlias), withNamespace = at(currentNamespace))
        val givenPasswordEntry2 = createPasswordEntryForTesting(withKeyBytes = bytesOf(givenAlias), withNamespace = at(targetNamespace))
        namespaceService.updateCurrentNamespace(at(currentNamespace))
        namespaceService.deploy(bytesOf("namespace"), at(targetNamespace))
        fakePasswordService(instance = passwordService, withPasswordEntries = listOf(givenPasswordEntry1, givenPasswordEntry2))
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(targetNamespace)))
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
