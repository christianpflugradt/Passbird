package de.pflugradts.passbird.application.commandhandling

import com.google.inject.Singleton
import de.pflugradts.passbird.application.commandhandling.command.AddNamespaceCommand
import de.pflugradts.passbird.application.commandhandling.command.AssignNamespaceCommand
import de.pflugradts.passbird.application.commandhandling.command.NullCommand
import de.pflugradts.passbird.application.commandhandling.command.SwitchNamespaceCommand
import de.pflugradts.passbird.application.commandhandling.command.ViewNamespaceCommand
import de.pflugradts.passbird.application.commandhandling.command.base.Command
import de.pflugradts.passbird.domain.model.namespace.NamespaceSlot.Companion.at
import de.pflugradts.passbird.domain.model.transfer.CharValue.Companion.charValueOf
import de.pflugradts.passbird.domain.model.transfer.Input

private const val MAX_COMMAND_SIZE = 3

@Singleton
class NamespaceCommandFactory() {
    fun constructFromInput(input: Input): Command {
        val command = input.command
        if (command.size > MAX_COMMAND_SIZE) {
            throw IllegalArgumentException("namespace command parameter not supported: ${input.command.slice(2).asString()}")
        } else if (command.size == 1 && input.data.isEmpty) {
            return ViewNamespaceCommand()
        } else if (command.size == 1 && !input.data.isEmpty) {
            return AssignNamespaceCommand(input)
        } else if (command.size == 2 && charValueOf(command.getChar(1)).isDigit) {
            return SwitchNamespaceCommand(at(command.getChar(1)))
        } else if (command.size > 2 && command.getChar(1) == CommandVariant.ADD.value) {
            return AddNamespaceCommand(at(command.getChar(2)))
        }
        return NullCommand()
    }
}
