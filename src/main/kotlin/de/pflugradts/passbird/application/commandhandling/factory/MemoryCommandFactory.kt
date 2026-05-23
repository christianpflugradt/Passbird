package de.pflugradts.passbird.application.commandhandling.factory

import de.pflugradts.passbird.application.commandhandling.command.GetMemoryCommand
import de.pflugradts.passbird.application.commandhandling.command.MemoryInfoCommand
import de.pflugradts.passbird.application.commandhandling.command.UseMemoryCommand
import de.pflugradts.passbird.application.commandhandling.command.ViewMemoryCommand
import de.pflugradts.passbird.domain.model.transfer.Input
import jakarta.inject.Singleton

@Singleton
class MemoryCommandFactory : SpecialCommandFactory() {
    override fun internalConstruct(input: Input) = input.command.let { cmd ->
        when {
            input.hasNoData() && cmd.size1() -> ViewMemoryCommand()
            input.hasNoData() && cmd.size2() && cmd.isInfoVariant() -> MemoryInfoCommand()
            input.hasNoData() && cmd.size2() && cmd.isSlotted() -> GetMemoryCommand(cmd.getSlot())
            input.hasData() && cmd.size2() && cmd.isSlotted() -> UseMemoryCommand(cmd.getSlot(), input)
            else -> null
        }
    }
}
