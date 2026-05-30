package de.pflugradts.passbird.application.commandhandling.factory
import de.pflugradts.passbird.application.commandhandling.command.DiscardProteinCommand
import de.pflugradts.passbird.application.commandhandling.command.GetProteinCommand
import de.pflugradts.passbird.application.commandhandling.command.ProteinInfoCommand
import de.pflugradts.passbird.application.commandhandling.command.SetProteinCommand
import de.pflugradts.passbird.application.commandhandling.command.ViewProteinStructuresCommand
import de.pflugradts.passbird.application.commandhandling.command.ViewProteinTypesCommand
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.transfer.Input
class ProteinCommandFactory : SpecialCommandFactory() {
    override fun internalConstruct(input: Input) = when {
        input.hasNoData() -> constructWithoutData(input.command)
        input.hasData() -> constructWithData(input.command, input)
        else -> null
    }
    private fun constructWithoutData(command: Shell) = when (command.size) {
        1 -> ProteinInfoCommand()
        2 -> command.takeIf { it.isInfoVariant() }?.let { ProteinInfoCommand() }
        else -> null
    }
    private fun constructWithData(command: Shell, input: Input) = when (command.size) {
        1 -> ViewProteinTypesCommand(input)
        2 -> constructSizeTwoCommand(command, input)
        3 -> constructSizeThreeCommand(command, input)
        else -> null
    }
    private fun constructSizeTwoCommand(command: Shell, input: Input) = when {
        command.isShowAllVariant() -> ViewProteinStructuresCommand(input)
        command.isSlotted() -> GetProteinCommand(command.getSlot(), input)
        else -> null
    }
    private fun constructSizeThreeCommand(command: Shell, input: Input) = command.takeIf { it.isSlotted() }?.let {
        when {
            it.isAddVariant() -> SetProteinCommand(it.getSlot(), input)
            it.isDiscardVariant() -> DiscardProteinCommand(it.getSlot(), input)
            else -> null
        }
    }
}
