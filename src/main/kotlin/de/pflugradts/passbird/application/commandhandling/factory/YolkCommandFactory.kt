package de.pflugradts.passbird.application.commandhandling.factory

import de.pflugradts.passbird.application.commandhandling.command.DiscardYolkCommand
import de.pflugradts.passbird.application.commandhandling.command.SetYolkCommand
import de.pflugradts.passbird.application.commandhandling.command.ViewYolkCommand
import de.pflugradts.passbird.application.commandhandling.command.YolkInfoCommand
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.transfer.Input

class YolkCommandFactory : SpecialCommandFactory() {
    override fun internalConstruct(input: Input) = when {
        input.hasNoData() -> constructWithoutData(input.command)
        input.hasData() -> constructWithData(input.command, input)
        else -> null
    }

    private fun constructWithoutData(command: Shell) = when (command.size) {
        1 -> YolkInfoCommand()
        2 -> command.takeIf { it.isInfoVariant() }?.let { YolkInfoCommand() }
        else -> null
    }

    private fun constructWithData(command: Shell, input: Input) = when (command.size) {
        1 -> ViewYolkCommand(input)

        2 -> when {
            command.isAddVariant() -> SetYolkCommand(input)
            command.isDiscardVariant() -> DiscardYolkCommand(input)
            else -> null
        }

        else -> null
    }
}
