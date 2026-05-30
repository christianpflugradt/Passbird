package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.InactivityTerminationRequestedException
import de.pflugradts.passbird.application.StdinTerminationRequestedException
import de.pflugradts.passbird.application.commandhandling.command.NullCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import strikt.api.expectThat
import strikt.assertions.containsExactly

class CommandHandlerBusTest {
    @Test
    fun `should dispatch command to matching command handler`() {
        // given
        val handledCommands = mutableListOf<NullCommand>()
        val command = NullCommand()
        val commandHandlerBus = CommandHandlerBus(setOf(CollectingCommandHandler(handledCommands)))

        // when
        commandHandlerBus.post(command)

        // then
        expectThat(handledCommands).containsExactly(command)
    }

    @Test
    fun `should rethrow inactivity termination requested by command handler`() {
        // given
        val commandHandlerBus = CommandHandlerBus(setOf(TerminatingCommandHandler(InactivityTerminationRequestedException())))

        // when / then
        assertThrows<InactivityTerminationRequestedException> { commandHandlerBus.post(NullCommand()) }
    }

    @Test
    fun `should rethrow stdin termination requested by command handler`() {
        // given
        val commandHandlerBus = CommandHandlerBus(setOf(TerminatingCommandHandler(StdinTerminationRequestedException())))

        // when / then
        assertThrows<StdinTerminationRequestedException> { commandHandlerBus.post(NullCommand()) }
    }

    @Test
    fun `should rethrow ordinary exception thrown by command handler`() {
        // given
        val exception = IllegalStateException("command failed")
        val commandHandlerBus = CommandHandlerBus(setOf(TerminatingCommandHandler(exception)))

        // when / then
        assertThrows<IllegalStateException> { commandHandlerBus.post(NullCommand()) }
    }

    private class TerminatingCommandHandler(
        private val exception: RuntimeException,
    ) : TypedCommandHandler<NullCommand>(NullCommand::class.java) {
        override fun handleCommand(command: NullCommand): Unit = throw exception
    }

    private class CollectingCommandHandler(
        private val handledCommands: MutableList<NullCommand>,
    ) : TypedCommandHandler<NullCommand>(NullCommand::class.java) {
        override fun handleCommand(command: NullCommand) {
            handledCommands.add(command)
        }
    }
}
