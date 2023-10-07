package de.pflugradts.passbird.application.commandhandling

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.application.commandhandling.command.CommandFactory
import de.pflugradts.passbird.application.commandhandling.command.CommandType
import de.pflugradts.passbird.domain.model.transfer.Input

@Singleton
class InputHandler @Inject constructor(
    @Inject private val commandBus: CommandBus,
    @Inject private val commandFactory: CommandFactory,
) {
    fun handleInput(input: Input) = commandBus.post(commandFactory.construct(CommandType.fromCommandBytes(input.command), input))
}
