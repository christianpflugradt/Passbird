package de.pflugradts.passbird.application.commandhandling.command

import de.pflugradts.passbird.application.commandhandling.command.base.AbstractInputCommand
import de.pflugradts.passbird.application.commandhandling.command.base.AbstractSingleCharInputCommand
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.transfer.Input

class MoveToNestCommand(input: Input) : AbstractSingleCharInputCommand(input)
class CustomSetCommand(input: Input) : AbstractSingleCharInputCommand(input)
class DiscardCommand(input: Input) : AbstractSingleCharInputCommand(input)
class GetCommand(input: Input) : AbstractSingleCharInputCommand(input)
class RenameCommand(input: Input) : AbstractSingleCharInputCommand(input)
class SetCommand(val slot: Slot, input: Input) : AbstractInputCommand(input)
class ViewCommand(input: Input) : AbstractSingleCharInputCommand(input)