package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.commandhandling.CommandType.Companion.resolveCommandTypeFrom
import de.pflugradts.passbird.application.commandhandling.factory.CommandFactory
import de.pflugradts.passbird.application.failure.CommandFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.domain.model.transfer.Input
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class CommandInputHandler @Inject constructor(
    private val commandBus: CommandBus,
    private val commandFactory: CommandFactory,
) : InputHandler {
    override fun handleInput(input: Input) {
        tryCatching {
            commandBus.post(commandFactory.construct(resolveCommandTypeFrom(input.command), input))
        }.onFailure { reportFailure(CommandFailure(it)) }
    }
}
