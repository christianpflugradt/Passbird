package de.pflugradts.passbird.application.commandhandling

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.StdinTerminationRequestedException
import de.pflugradts.passbird.application.commandhandling.command.NullCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CommandHandlerBusTest {
    @Test
    fun `should rethrow stdin termination requested by command handler`() {
        // given
        val commandHandlerBus = CommandHandlerBus(setOf(TerminatingCommandHandler(StdinTerminationRequestedException())))

        // when / then
        assertThrows<StdinTerminationRequestedException> { commandHandlerBus.post(NullCommand()) }
    }

    private class TerminatingCommandHandler(
        private val exception: RuntimeException,
    ) : CommandHandler {
        @Subscribe
        fun handle(command: NullCommand): Unit = throw exception
    }
}
