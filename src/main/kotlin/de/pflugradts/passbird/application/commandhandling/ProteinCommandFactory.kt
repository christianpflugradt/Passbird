package de.pflugradts.passbird.application.commandhandling

import com.google.inject.Singleton
import de.pflugradts.passbird.application.commandhandling.command.DiscardProteinCommand
import de.pflugradts.passbird.application.commandhandling.command.GetProteinCommand
import de.pflugradts.passbird.application.commandhandling.command.ProteinInfoCommand
import de.pflugradts.passbird.application.commandhandling.command.SetProteinCommand
import de.pflugradts.passbird.application.commandhandling.command.ViewProteinStructuresCommand
import de.pflugradts.passbird.application.commandhandling.command.ViewProteinTypesCommand
import de.pflugradts.passbird.domain.model.transfer.Input

@Singleton
class ProteinCommandFactory : SpecialCommandFactory() {
    override fun internalConstruct(input: Input) = input.command.let { cmd ->
        when {
            input.hasNoData() && (cmd.size1() || (cmd.size2() && cmd.isInfoVariant())) -> ProteinInfoCommand()
            input.hasData() && cmd.size1() -> ViewProteinTypesCommand(input)
            input.hasData() && cmd.size2() && cmd.isShowAllVariant() -> ViewProteinStructuresCommand(input)
            input.hasData() && cmd.size2() && cmd.isSlotted() -> GetProteinCommand(cmd.getSlot(), input)
            input.hasData() && cmd.size3() && cmd.isAddVariant() && cmd.isSlotted() -> SetProteinCommand(cmd.getSlot(), input)
            input.hasData() && cmd.size3() && cmd.isDiscardVariant() && cmd.isSlotted() -> DiscardProteinCommand(cmd.getSlot(), input)
            else -> null
        }
    }
}
