package de.pflugradts.passbird.application.commandhandling.factory
import de.pflugradts.passbird.application.commandhandling.command.AddFavoriteCommand
import de.pflugradts.passbird.application.commandhandling.command.DiscardFavoriteCommand
import de.pflugradts.passbird.application.commandhandling.command.FavoriteInfoCommand
import de.pflugradts.passbird.application.commandhandling.command.GetFavoriteCommand
import de.pflugradts.passbird.application.commandhandling.command.UseFavoriteCommand
import de.pflugradts.passbird.application.commandhandling.command.ViewFavoriteCommand
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.transfer.Input
class FavoriteCommandFactory : SpecialCommandFactory() {
    override fun internalConstruct(input: Input) = when {
        input.hasNoData() -> constructWithoutData(input.command)
        input.hasData() -> constructWithData(input.command, input)
        else -> null
    }
    private fun constructWithoutData(command: Shell) = when (command.size) {
        1 -> ViewFavoriteCommand()
        2 -> constructSizeTwoCommand(command)
        3 -> constructSizeThreeCommand(command)
        else -> null
    }
    private fun constructWithData(command: Shell, input: Input) = when (command.size) {
        2 -> command.takeIf { it.isSlotted() }?.let { UseFavoriteCommand(it.getSlot(), input) }
        3 -> constructAddCommand(command, input)
        else -> null
    }
    private fun constructSizeTwoCommand(command: Shell) = when {
        command.isInfoVariant() -> FavoriteInfoCommand()
        command.isSlotted() -> GetFavoriteCommand(command.getSlot())
        else -> null
    }
    private fun constructSizeThreeCommand(command: Shell) = command
        .takeIf { it.isDiscardVariant() && it.isSlotted() }
        ?.let { DiscardFavoriteCommand(it.getSlot()) }
    private fun constructAddCommand(command: Shell, input: Input) = command
        .takeIf { it.isAddVariant() && it.isSlotted() }
        ?.let { AddFavoriteCommand(it.getSlot(), input) }
}
