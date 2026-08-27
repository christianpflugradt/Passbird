package de.pflugradts.passbird.application.commandhandling.factory

import de.pflugradts.passbird.application.commandhandling.CommandVariant
import de.pflugradts.passbird.application.commandhandling.command.DiscardCommand
import de.pflugradts.passbird.application.commandhandling.command.ViewTrashCommand
import de.pflugradts.passbird.domain.model.transfer.Input

class DiscardCommandFactory : SpecialCommandFactory() {
    override fun internalConstruct(input: Input) = input.command.let { command ->
        when {
            command.size1() && input.hasNoData() -> ViewTrashCommand()

            command.size1() && input.hasData() -> DiscardCommand(force = false, input)

            command.size2() && command.getChar(1) == CommandVariant.FORCE.value && input.hasData() ->
                DiscardCommand(force = true, input)

            else -> null
        }
    }
}
