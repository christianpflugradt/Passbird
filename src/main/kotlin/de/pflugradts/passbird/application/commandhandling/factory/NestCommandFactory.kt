package de.pflugradts.passbird.application.commandhandling.factory

import com.google.inject.Singleton
import de.pflugradts.passbird.application.commandhandling.command.AddNestCommand
import de.pflugradts.passbird.application.commandhandling.command.DiscardNestCommand
import de.pflugradts.passbird.application.commandhandling.command.MoveToNestCommand
import de.pflugradts.passbird.application.commandhandling.command.SwitchNestCommand
import de.pflugradts.passbird.application.commandhandling.command.ViewNestCommand
import de.pflugradts.passbird.domain.model.transfer.Input

@Singleton
class NestCommandFactory : SpecialCommandFactory() {
    override fun internalConstruct(input: Input) = input.command.let { cmd ->
        when {
            input.hasNoData() && cmd.size1() -> ViewNestCommand()
            input.hasData() && cmd.size1() -> MoveToNestCommand(input)
            input.hasNoData() && cmd.size2() && cmd.isSlotted() -> SwitchNestCommand(cmd.getSlot())
            input.hasNoData() && cmd.size3() && cmd.isAddVariant() && cmd.isSlotted() -> AddNestCommand(cmd.getSlot())
            input.hasNoData() && cmd.size3() && cmd.isDiscardVariant() && cmd.isSlotted() -> DiscardNestCommand(cmd.getSlot())
            else -> null
        }
    }
}
