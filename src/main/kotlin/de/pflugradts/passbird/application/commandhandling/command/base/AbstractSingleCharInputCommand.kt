package de.pflugradts.passbird.application.commandhandling.command.base

import de.pflugradts.passbird.domain.model.transfer.Input

private const val MAX_COMMAND_SIZE = 1

abstract class AbstractSingleCharInputCommand protected constructor(private val input: Input) : AbstractInputCommand(input) {
    init {
        require(input.command.size <= MAX_COMMAND_SIZE) {
            ("Parameter for command '${input.command.getChar(0)}' not supported: ${input.command.slice(1).asString()}")
        }
    }
}
