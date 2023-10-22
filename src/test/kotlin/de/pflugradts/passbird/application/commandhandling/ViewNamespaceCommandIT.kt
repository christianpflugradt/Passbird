package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.namespace.ViewNamespaceCommandHandler
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.Companion.at
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
import strikt.assertions.isTrue

class ViewNamespaceCommandIT {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val namespaceService = createNamespaceServiceForTesting()
    private val viewNamespaceCommandHandler = ViewNamespaceCommandHandler(namespaceService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(viewNamespaceCommandHandler)

    @Test
    fun `should print info`() {
        // given
        val input = inputOf(bytesOf("n"))
        val outputSlot = slot<Output>()

        // when
        inputHandler.handleInput(input)

        // then
        verify(exactly = 1) { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.isCaptured).isTrue()
        expectThat(outputSlot.captured.bytes.asString()) contains "Available namespace commands"
    }

    @Test
    fun `should print default namespace if current`() {
        // given
        val input = inputOf(bytesOf("n"))
        val outputSlot = slot<Output>()

        // when
        inputHandler.handleInput(input)

        // then
        verify(exactly = 1) { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.isCaptured).isTrue()
        expectThat(outputSlot.captured.bytes.asString()) contains "Current namespace: Default"
    }

    @Test
    fun `should print deployed namespace`() {
        // given
        val input = inputOf(bytesOf("n"))
        val deployedNamespaceSlot = 3
        val deployedNamespace = "mynamespace"
        namespaceService.deploy(bytesOf(deployedNamespace), at(deployedNamespaceSlot))
        val outputSlot = slot<Output>()

        // when
        inputHandler.handleInput(input)

        // then
        verify(exactly = 1) { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.isCaptured).isTrue()
        expectThat(outputSlot.captured.bytes.asString()) contains "$deployedNamespaceSlot: $deployedNamespace"
    }
}
