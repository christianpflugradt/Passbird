package de.pflugradts.passbird.application.commandhandling.command

import de.pflugradts.passbird.domain.model.transfer.Input

abstract class AbstractFilenameCommand(val input: Input) : AbstractInputCommand(input) {
    override val argument get() = input.bytes.slice(1)
}
