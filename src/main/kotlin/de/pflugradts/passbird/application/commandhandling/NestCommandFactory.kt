package de.pflugradts.passbird.application.commandhandling

import com.google.inject.Singleton
import de.pflugradts.passbird.application.commandhandling.CommandVariant.ADD
import de.pflugradts.passbird.application.commandhandling.CommandVariant.DISCARD
import de.pflugradts.passbird.application.commandhandling.command.AddNestCommand
import de.pflugradts.passbird.application.commandhandling.command.DiscardNestCommand
import de.pflugradts.passbird.application.commandhandling.command.MoveToNestCommand
import de.pflugradts.passbird.application.commandhandling.command.NullCommand
import de.pflugradts.passbird.application.commandhandling.command.SwitchNestCommand
import de.pflugradts.passbird.application.commandhandling.command.ViewNestCommand
import de.pflugradts.passbird.application.commandhandling.command.base.Command
import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.nestSlotAt
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.transfer.Input

private const val MAX_COMMAND_SIZE = 3

@Singleton
class NestCommandFactory() {
    fun constructFromInput(input: Input): Command {
        val command = input.command
        if (command.size > MAX_COMMAND_SIZE) {
            throw IllegalArgumentException("Nest command parameter not supported: ${input.command.slice(2).asString()}")
        } else if (command.size == 1 && input.data.isEmpty) {
            return ViewNestCommand()
        } else if (command.size == 1 && !input.data.isEmpty) {
            return MoveToNestCommand(input)
        } else if (command.size == 2 && input.data.isEmpty && command.getChar(1).isDigit()) {
            return SwitchNestCommand(nestSlotAt(command.getChar(1)))
        } else if (command.size == 3 && input.data.isEmpty && command.getChar(1) == ADD.value && command.isDigit(2)) {
            return AddNestCommand(nestSlotAt(command.getChar(2)))
        } else if (command.size == 3 && input.data.isEmpty && command.getChar(1) == DISCARD.value && command.isDigit(2)) {
            return DiscardNestCommand(nestSlotAt(command.getChar(2)))
        }
        return NullCommand()
    }
}

private fun Shell.isDigit(index: Int) = getChar(index).isDigit()
