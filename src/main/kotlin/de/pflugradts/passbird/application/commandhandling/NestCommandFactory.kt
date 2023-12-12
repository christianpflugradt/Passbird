package de.pflugradts.passbird.application.commandhandling

import com.google.inject.Singleton
import de.pflugradts.passbird.application.commandhandling.command.AddNestCommand
import de.pflugradts.passbird.application.commandhandling.command.AssignNestCommand
import de.pflugradts.passbird.application.commandhandling.command.NullCommand
import de.pflugradts.passbird.application.commandhandling.command.SwitchNestCommand
import de.pflugradts.passbird.application.commandhandling.command.ViewNestCommand
import de.pflugradts.passbird.application.commandhandling.command.base.Command
import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.at
import de.pflugradts.passbird.domain.model.shell.PlainValue.Companion.plainValueOf
import de.pflugradts.passbird.domain.model.transfer.Input

private const val MAX_COMMAND_SIZE = 3

@Singleton
class NestCommandFactory() {
    fun constructFromInput(input: Input): Command {
        val command = input.command
        if (command.size > MAX_COMMAND_SIZE) {
            throw IllegalArgumentException("namespace command parameter not supported: ${input.command.slice(2).asString()}")
        } else if (command.size == 1 && input.data.isEmpty) {
            return ViewNestCommand()
        } else if (command.size == 1 && !input.data.isEmpty) {
            return AssignNestCommand(input)
        } else if (command.size == 2 && plainValueOf(command.getChar(1)).isDigit) {
            return SwitchNestCommand(at(command.getChar(1)))
        } else if (command.size == 3 && command.getChar(1) == CommandVariant.ADD.value && command.getChar(2).isDigit()) {
            return AddNestCommand(at(command.getChar(2)))
        }
        return NullCommand()
    }
}
