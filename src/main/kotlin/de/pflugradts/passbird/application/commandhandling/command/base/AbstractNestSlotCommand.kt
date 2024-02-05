package de.pflugradts.passbird.application.commandhandling.command.base

import de.pflugradts.passbird.domain.model.slot.Slot

abstract class AbstractNestSlotCommand(val slot: Slot) : Command
