package de.pflugradts.passbird.application.commandhandling.command.base

import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.transfer.Input

abstract class AbstractInputCommand protected constructor(private val input: Input) : Command {
    open val argument: Shell get() = input.data
    fun invalidateInput() = input.invalidate()
}
