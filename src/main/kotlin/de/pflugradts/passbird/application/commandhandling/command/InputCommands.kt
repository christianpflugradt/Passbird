package de.pflugradts.passbird.application.commandhandling.command

import de.pflugradts.passbird.application.commandhandling.command.base.AbstractInputCommand
import de.pflugradts.passbird.application.commandhandling.command.base.AbstractSingleCharInputCommand
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.transfer.Input

class MoveToNestCommand(input: Input) : AbstractSingleCharInputCommand(input)
class CustomSetCommand(input: Input) : AbstractSingleCharInputCommand(input)
class DiscardCommand(input: Input) : AbstractSingleCharInputCommand(input)
class GetCommand(input: Input) : AbstractSingleCharInputCommand(input)
class RenameCommand(input: Input) : AbstractSingleCharInputCommand(input)
class SetCommand(val slot: NestSlot, input: Input) : AbstractInputCommand(input) // FIXME refactor nestSlot to general slot
class ViewCommand(input: Input) : AbstractSingleCharInputCommand(input)
