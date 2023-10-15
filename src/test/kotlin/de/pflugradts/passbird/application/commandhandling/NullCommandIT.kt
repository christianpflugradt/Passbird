package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.commandhandling.command.NullCommand
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.emptyBytes
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test

class NullCommandIT {
    private val commandBus = spyk(CommandBus(emptySet()))
    private var inputHandler = createInputHandlerFor(commandBus)

    @Test
    fun `should handle unknown command`() {
        // given
        val input = inputOf(bytesOf("?"))

        // when
        inputHandler.handleInput(input)

        // then
        verify { commandBus.post(any(NullCommand::class)) }
    }

    @Test
    fun `should handle empty command`() {
        // given
        val input = inputOf(emptyBytes())

        // when
        inputHandler.handleInput(input)

        // then
        verify { commandBus.post(any(NullCommand::class)) }
    }
}
