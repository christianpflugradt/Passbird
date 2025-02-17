package de.pflugradts.passbird.application.commandhandling.command

import de.pflugradts.passbird.application.commandhandling.command.base.AbstractInputCommand
import de.pflugradts.passbird.application.commandhandling.command.base.AbstractSingleCharInputCommand
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.transfer.Input

class CustomSetCommand(input: Input) : AbstractSingleCharInputCommand(input)
class DiscardCommand(input: Input) : AbstractSingleCharInputCommand(input)
class DiscardProteinCommand(val slot: Slot, input: Input) : AbstractInputCommand(input)
class GetCommand(input: Input) : AbstractSingleCharInputCommand(input)
class GetProteinCommand(val slot: Slot, input: Input) : AbstractInputCommand(input)
class MoveToNestCommand(input: Input) : AbstractSingleCharInputCommand(input)
class RenameCommand(input: Input) : AbstractSingleCharInputCommand(input)
class SetCommand(val slot: Slot, input: Input) : AbstractInputCommand(input)
class SetProteinCommand(val slot: Slot, input: Input) : AbstractInputCommand(input)
class UseMemoryCommand(val slot: Slot, input: Input) : AbstractInputCommand(input)
class ViewCommand(input: Input) : AbstractSingleCharInputCommand(input)
class ViewProteinStructuresCommand(input: Input) : AbstractInputCommand(input)
class ViewProteinTypesCommand(input: Input) : AbstractSingleCharInputCommand(input)
