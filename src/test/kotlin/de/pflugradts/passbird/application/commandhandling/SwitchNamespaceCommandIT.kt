package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.namespace.SwitchNamespaceCommandHandler
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.createNamespaceServiceForTesting
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

class SwitchNamespaceCommandIT {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val namespaceService = createNamespaceServiceForTesting()
    private val switchNamespaceCommandHandler = SwitchNamespaceCommandHandler(namespaceService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(switchNamespaceCommandHandler)

    @Test
    fun `should handle switch namespace command`() {
        // given
        val givenNamespace = NamespaceSlot.N1
        namespaceService.deploy(bytesOf("namespace"), givenNamespace)
        val input = inputOf(bytesOf("n" + givenNamespace.index()))

        // when
        inputHandler.handleInput(input)

        // then
        verify(exactly = 0) { userInterfaceAdapterPort.send(any()) }
        expectThat(namespaceService.getCurrentNamespace().slot) isEqualTo givenNamespace
    }

    @Test
    fun `should do nothing if namespace is already current`() {
        // given
        val givenNamespace = NamespaceSlot.N1
        namespaceService.deploy(bytesOf("namespace"), givenNamespace)
        namespaceService.updateCurrentNamespace(givenNamespace)
        val input = inputOf(bytesOf("n" + givenNamespace.index()))
        val outputSlot = slot<Output>()

        // when
        inputHandler.handleInput(input)

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.isCaptured).isTrue()
        expectThat(outputSlot.captured.bytes.asString()) contains "is already the current namespace"
    }

    @Test
    fun `should do nothing if namespace is not deployed`() {
        // given
        val givenNamespace = NamespaceSlot.N1
        val input = inputOf(bytesOf("n" + givenNamespace.index()))
        expectThat(namespaceService.atSlot(givenNamespace).isEmpty).isTrue()
        val outputSlot = slot<Output>()

        // when
        inputHandler.handleInput(input)

        // then
        verify { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.isCaptured).isTrue()
        expectThat(outputSlot.captured.bytes.asString()) contains "Specified namespace does not exist"
    }
}
