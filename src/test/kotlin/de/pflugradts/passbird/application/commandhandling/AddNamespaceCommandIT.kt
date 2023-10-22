package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.namespace.AddNamespaceCommandHandler
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.Companion.at
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.NamespaceService
import de.pflugradts.passbird.domain.service.createNamespaceServiceForTesting
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue
import kotlin.jvm.optionals.getOrNull

class AddNamespaceCommandIT {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val namespaceService = createNamespaceServiceForTesting()
    private val addNamespaceCommandHandler = AddNamespaceCommandHandler(namespaceService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(addNamespaceCommandHandler)

    @Test
    fun `should handle add namespace command`() {
        // given
        val slotIndex = 1
        val givenInput = bytesOf("n+$slotIndex")
        val slotFromInput = at(slotIndex)
        val referenceNamespace = bytesOf("mynamespace")
        val givenNamespace = bytesOf("mynamespace")
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(givenNamespace)))

        // when
        inputHandler.handleInput(inputOf(givenInput))

        // then
        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
        expectThat(namespaceService.namespaceBytesAtSlot(slotFromInput)) isEqualTo referenceNamespace
        expectThat(givenNamespace) isNotEqualTo referenceNamespace
    }

    @Test
    fun `should update existing namespace`() {
        // given
        val slotIndex = 1
        val input = inputOf(bytesOf("n+$slotIndex"))
        val slotFromInput = at(slotIndex)
        val referenceNamespace = bytesOf("mynamespace")
        val givenNamespace = bytesOf("mynamespace")
        val otherNamespace = bytesOf("othernamespace")
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(givenNamespace)))
        namespaceService.deploy(otherNamespace, slotFromInput)
        expectThat(namespaceService.namespaceBytesAtSlot(slotFromInput)) isEqualTo otherNamespace

        // when
        inputHandler.handleInput(input)

        // then
        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
        expectThat(namespaceService.namespaceBytesAtSlot(slotFromInput)) isEqualTo referenceNamespace
        expectThat(givenNamespace) isNotEqualTo referenceNamespace
    }

    @Test
    fun `should handle empty input`() {
        // given
        val slotIndex = 1
        val input = inputOf(bytesOf("n+$slotIndex"))
        val slotFromInput = at(slotIndex)
        val givenNamespace = bytesOf("")
        fakeUserInterfaceAdapterPort(instance = userInterfaceAdapterPort, withTheseInputs = listOf(inputOf(givenNamespace)))
        val outputSlot = slot<Output>()

        // when
        inputHandler.handleInput(input)

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.bytes.asString()) contains "Empty input"
        expectThat(namespaceService.atSlot(slotFromInput).isEmpty).isTrue()
    }

    @Test
    fun `should handle add namespace command for default slot`() {
        // given
        val slotIndex = 0
        val input = inputOf(bytesOf("n+$slotIndex"))
        val outputSlot = slot<Output>()

        // when
        inputHandler.handleInput(input)

        // then
        verify(exactly = 0) { userInterfaceAdapterPort.receive(any()) }
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.captured.bytes.asString()) contains "Default namespace cannot be replaced"
    }

    private fun NamespaceService.namespaceBytesAtSlot(slot: NamespaceSlot) = atSlot(slot).getOrNull()?.bytes ?: emptyBytes()
}
