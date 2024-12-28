package de.pflugradts.passbird.application.commandhandling.memory

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.InputHandler
import de.pflugradts.passbird.application.commandhandling.createInputHandlerFor
import de.pflugradts.passbird.application.commandhandling.handler.memory.UseMemoryCommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.fakePasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag(INTEGRATION)
class UseMemoryCommandTest {

    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>(relaxed = true)
    private val passwordService = mockk<PasswordService>()
    private val mockedInputHandler = mockk<InputHandler>()
    private val useMemoryCommandHandler = UseMemoryCommandHandler(mockedInputHandler, passwordService, userInterfaceAdapterPort)
    private val inputHandler = createInputHandlerFor(useMemoryCommandHandler)

    @Test
    fun `should handle use memory command`() {
        // given
        val slot = DEFAULT
        val memorizedEggId = "eggId"
        val forwardCommand = "p0"
        val command = shellOf("m${slot.index()}$forwardCommand")
        fakePasswordService(instance = passwordService, withMemory = mapOf(slot to memorizedEggId))

        // when
        inputHandler.handleInput(inputOf(command))

        // then
        verify(exactly = 1) { mockedInputHandler.handleInput(inputOf(shellOf("$forwardCommand$memorizedEggId"))) }
    }

    @Test
    fun `should handle use memory command on empty memory slot`() {
        // given
        val slot = DEFAULT
        val command = shellOf("m${slot.index()}p0")
        fakePasswordService(instance = passwordService, withMemory = emptyMap())

        // when
        inputHandler.handleInput(inputOf(command))

        // then
        verify { mockedInputHandler wasNot Called }
        verify(exactly = 1) { userInterfaceAdapterPort.send(outputOf(shellOf("Memory entry at slot ${slot.index()} does not exist."))) }
    }
}
