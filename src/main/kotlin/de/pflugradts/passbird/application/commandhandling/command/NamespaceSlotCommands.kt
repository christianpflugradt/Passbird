package de.pflugradts.passbird.application.commandhandling.command

import de.pflugradts.passbird.application.commandhandling.command.base.AbstractNamespaceSlotCommand
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot

class AddNamespaceCommand(namespaceSlot: NamespaceSlot) : AbstractNamespaceSlotCommand(namespaceSlot)
class SwitchNamespaceCommand(namespaceSlot: NamespaceSlot) : AbstractNamespaceSlotCommand(namespaceSlot)
