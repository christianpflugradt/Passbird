package de.pflugradts.passbird.application.commandhandling.command

import de.pflugradts.passbird.application.commandhandling.command.base.AbstractFilenameCommand
import de.pflugradts.passbird.application.commandhandling.command.base.AbstractSingleCharInputCommand
import de.pflugradts.passbird.domain.model.transfer.Input

class AssignNamespaceCommand(input: Input) : AbstractSingleCharInputCommand(input)
class CustomSetCommand(input: Input) : AbstractSingleCharInputCommand(input)
class DiscardCommand(input: Input) : AbstractFilenameCommand(input)
class GetCommand(input: Input) : AbstractFilenameCommand(input)
class RenameCommand(input: Input) : AbstractFilenameCommand(input)
class SetCommand(input: Input) : AbstractFilenameCommand(input)
class ViewCommand(input: Input) : AbstractFilenameCommand(input)
