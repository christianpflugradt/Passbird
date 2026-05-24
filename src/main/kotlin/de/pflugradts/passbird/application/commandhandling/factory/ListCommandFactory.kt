package de.pflugradts.passbird.application.commandhandling.factory

import de.pflugradts.passbird.application.commandhandling.command.ListCommand
import de.pflugradts.passbird.domain.model.transfer.Input
import jakarta.inject.Singleton

@Singleton
class ListCommandFactory : SpecialCommandFactory() {
    override fun internalConstruct(input: Input) = input.command.let { cmd ->
        when {
            cmd.size1() -> ListCommand(showAll = false, input)
            cmd.size2() && cmd.isShowAllVariant() -> ListCommand(showAll = true, input)
            else -> null
        }
    }
}
