package de.pflugradts.passbird.application.commandhandling

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.application.commandhandling.CommandType.Companion.resolveCommandTypeFrom
import de.pflugradts.passbird.domain.model.transfer.Input

@Singleton
class InputHandler @Inject constructor(
    @Inject private val commandBus: CommandBus,
    @Inject private val commandFactory: CommandFactory,
) {
    fun handleInput(input: Input) = commandBus.post(commandFactory.construct(resolveCommandTypeFrom(input.command), input))
}
