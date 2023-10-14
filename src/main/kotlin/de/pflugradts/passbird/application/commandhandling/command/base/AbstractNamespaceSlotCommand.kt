package de.pflugradts.passbird.application.commandhandling.command.base

import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot

abstract class AbstractNamespaceSlotCommand(val slot: NamespaceSlot) : Command
