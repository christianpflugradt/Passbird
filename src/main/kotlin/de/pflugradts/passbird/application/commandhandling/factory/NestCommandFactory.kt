package de.pflugradts.passbird.application.commandhandling.factory
import de.pflugradts.passbird.application.commandhandling.command.AddNestCommand
import de.pflugradts.passbird.application.commandhandling.command.DiscardNestCommand
import de.pflugradts.passbird.application.commandhandling.command.MoveToNestCommand
import de.pflugradts.passbird.application.commandhandling.command.SwitchNestCommand
import de.pflugradts.passbird.application.commandhandling.command.ViewNestCommand
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.transfer.Input
class NestCommandFactory : SpecialCommandFactory() {
    override fun internalConstruct(input: Input) = when {
        input.hasNoData() -> constructWithoutData(input.command)
        input.hasData() -> constructWithData(input)
        else -> null
    }
    private fun constructWithoutData(command: Shell) = when (command.size) {
        1 -> ViewNestCommand()
        2 -> command.takeIf { it.isSlotted() }?.let { SwitchNestCommand(it.getSlot()) }
        3 -> constructVariant(command)
        else -> null
    }
    private fun constructWithData(input: Input) = input.command.takeIf { it.size1() }?.let { MoveToNestCommand(input) }
    private fun constructVariant(command: Shell) = command.takeIf { it.isSlotted() }?.let {
        when {
            it.isAddVariant() -> AddNestCommand(it.getSlot())
            it.isDiscardVariant() -> DiscardNestCommand(it.getSlot())
            else -> null
        }
    }
}
