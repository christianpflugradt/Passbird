package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.commandhandling.handler.QuitCommandHandler
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class QuitCommandIT {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val bootable = mockk<Bootable>()
    private val quitCommandHandler = QuitCommandHandler(userInterfaceAdapterPort, bootable)
    private val inputHandler = InputHandlerTestFactory.setupInputHandlerFor(quitCommandHandler)

    @Test
    fun `should handle quit command`() {
        // given
        val input = inputOf(bytesOf("q"))

        // when
        inputHandler.handleInput(input)

        // then
        verify(exactly = 1) { userInterfaceAdapterPort.send(any()) }
        verify(exactly = 1) { bootable.terminate(any()) }
    }
}
