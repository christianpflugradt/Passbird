package de.pflugradts.passbird.application.commandhandling.command

import de.pflugradts.passbird.application.commandhandling.command.base.AbstractNestSlotCommand
import de.pflugradts.passbird.domain.model.nest.NestSlot

class AddNestCommand(nestSlot: NestSlot) : AbstractNestSlotCommand(nestSlot)
class DiscardNestCommand(nestSlot: NestSlot) : AbstractNestSlotCommand(nestSlot)
class SwitchNestCommand(nestSlot: NestSlot) : AbstractNestSlotCommand(nestSlot)
