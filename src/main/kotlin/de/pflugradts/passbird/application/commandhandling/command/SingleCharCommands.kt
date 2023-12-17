package de.pflugradts.passbird.application.commandhandling.command

import de.pflugradts.passbird.application.commandhandling.command.base.AbstractSingleCharInputCommand
import de.pflugradts.passbird.domain.model.transfer.Input

class AssignNestCommand(input: Input) : AbstractSingleCharInputCommand(input)
class CustomSetCommand(input: Input) : AbstractSingleCharInputCommand(input)
class DiscardCommand(input: Input) : AbstractSingleCharInputCommand(input)
class GetCommand(input: Input) : AbstractSingleCharInputCommand(input)
class RenameCommand(input: Input) : AbstractSingleCharInputCommand(input)
class SetCommand(input: Input) : AbstractSingleCharInputCommand(input)
class ViewCommand(input: Input) : AbstractSingleCharInputCommand(input)
