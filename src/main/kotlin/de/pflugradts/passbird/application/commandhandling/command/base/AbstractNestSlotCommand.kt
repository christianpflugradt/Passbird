package de.pflugradts.passbird.application.commandhandling.command.base

import de.pflugradts.passbird.domain.model.nest.NestSlot

abstract class AbstractNestSlotCommand(val nestSlot: NestSlot) : Command
