package de.pflugradts.passbird.application.commandhandling.command.base

import de.pflugradts.passbird.domain.model.slot.Slot

abstract class AbstractSlotCommand(val slot: Slot) : Command
