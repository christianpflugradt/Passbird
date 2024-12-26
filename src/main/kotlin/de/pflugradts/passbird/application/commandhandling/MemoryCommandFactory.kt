package de.pflugradts.passbird.application.commandhandling

import com.google.inject.Singleton
import de.pflugradts.passbird.application.commandhandling.command.ViewMemoryCommand
import de.pflugradts.passbird.domain.model.transfer.Input

@Singleton
class MemoryCommandFactory : SpecialCommandFactory() {
    override fun internalConstruct(input: Input) = input.command.let { cmd ->
        when {
            input.hasNoData() && cmd.size1() -> ViewMemoryCommand()
            else -> null
        }
    }
}
