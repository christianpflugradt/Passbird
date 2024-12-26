package de.pflugradts.passbird.application.commandhandling.factory

import com.google.inject.Singleton
import de.pflugradts.passbird.application.commandhandling.command.GetMemoryCommand
import de.pflugradts.passbird.application.commandhandling.command.ViewMemoryCommand
import de.pflugradts.passbird.domain.model.transfer.Input

@Singleton
class MemoryCommandFactory : SpecialCommandFactory() {
    override fun internalConstruct(input: Input) = input.command.let { cmd ->
        when {
            input.hasNoData() && cmd.size1() -> ViewMemoryCommand()
            input.hasNoData() && cmd.size2() && cmd.isSlotted() -> GetMemoryCommand(cmd.getSlot())
            else -> null
        }
    }
}
