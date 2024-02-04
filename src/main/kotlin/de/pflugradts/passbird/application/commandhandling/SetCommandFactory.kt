package de.pflugradts.passbird.application.commandhandling

import com.google.inject.Singleton
import de.pflugradts.passbird.application.commandhandling.command.NullCommand
import de.pflugradts.passbird.application.commandhandling.command.SetCommand
import de.pflugradts.passbird.application.commandhandling.command.SetInfoCommand
import de.pflugradts.passbird.application.commandhandling.command.base.Command
import de.pflugradts.passbird.domain.model.nest.NestSlot
import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.nestSlotAt
import de.pflugradts.passbird.domain.model.transfer.Input

private const val MAX_COMMAND_SIZE = 3

@Singleton
class SetCommandFactory {
    fun constructFromInput(input: Input): Command {
        val command = input.command
        if (command.size > MAX_COMMAND_SIZE) {
            throw IllegalArgumentException("Set command parameter not supported: ${input.command.slice(2).asString()}")
        } else if (command.size == 1 && input.data.isNotEmpty) {
            return SetCommand(NestSlot.DEFAULT, input)
        } else if (command.size == 2 && input.data.isEmpty && command.getChar(1) == CommandVariant.INFO.value) {
            return SetInfoCommand()
        } else if (command.size == 2 && input.data.isNotEmpty && command.getChar(1).isDigit()) {
            return SetCommand(nestSlotAt(command.getChar(1)), input)
        }
        return NullCommand()
    }
}
