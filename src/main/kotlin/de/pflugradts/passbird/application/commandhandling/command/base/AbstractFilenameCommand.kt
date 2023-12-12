package de.pflugradts.passbird.application.commandhandling.command.base

import de.pflugradts.passbird.domain.model.transfer.Input

abstract class AbstractFilenameCommand(private val input: Input) : AbstractInputCommand(input) {
    override val argument get() = input.shell.slice(1)
}
