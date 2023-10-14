package de.pflugradts.passbird.application.commandhandling.command

import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Input

abstract class AbstractInputCommand protected constructor(private val input: Input) : Command {
    open val argument: Bytes get() = input.data
    fun invalidateInput() = input.invalidate()
}
