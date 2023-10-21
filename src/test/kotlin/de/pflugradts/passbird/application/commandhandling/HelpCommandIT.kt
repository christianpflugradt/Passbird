package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.handler.HelpCommandHandler
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.model.transfer.Output
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isTrue

class HelpCommandIT {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val quitCommandHandler = HelpCommandHandler(userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(quitCommandHandler)

    @Test
    fun `should handle help command`() {
        // given
        val input = Input.inputOf(Bytes.bytesOf("h"))
        val outputSlot = slot<Output>()

        // when
        inputHandler.handleInput(input)

        // then
        verify(exactly = 1) { userInterfaceAdapterPort.send(capture(outputSlot)) }
        expectThat(outputSlot.isCaptured).isTrue()
        expectThat(outputSlot.captured.bytes.asString()).contains("Usage")
    }
}