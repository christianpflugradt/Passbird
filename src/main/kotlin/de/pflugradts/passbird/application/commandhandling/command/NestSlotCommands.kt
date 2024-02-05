package de.pflugradts.passbird.application.commandhandling.command

import de.pflugradts.passbird.application.commandhandling.command.base.AbstractNestSlotCommand
import de.pflugradts.passbird.domain.model.slot.Slot

class AddNestCommand(slot: Slot) : AbstractNestSlotCommand(slot)
class DiscardNestCommand(slot: Slot) : AbstractNestSlotCommand(slot)
class SwitchNestCommand(slot: Slot) : AbstractNestSlotCommand(slot)
