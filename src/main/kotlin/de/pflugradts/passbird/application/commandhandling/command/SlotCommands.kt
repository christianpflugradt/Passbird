package de.pflugradts.passbird.application.commandhandling.command

import de.pflugradts.passbird.application.commandhandling.command.base.AbstractSlotCommand
import de.pflugradts.passbird.domain.model.slot.Slot

class AddNestCommand(slot: Slot) : AbstractSlotCommand(slot)
class DiscardNestCommand(slot: Slot) : AbstractSlotCommand(slot)
class SwitchNestCommand(slot: Slot) : AbstractSlotCommand(slot)
