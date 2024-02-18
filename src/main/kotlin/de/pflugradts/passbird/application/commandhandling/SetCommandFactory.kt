package de.pflugradts.passbird.application.commandhandling

import com.google.inject.Singleton
import de.pflugradts.passbird.application.commandhandling.command.SetCommand
import de.pflugradts.passbird.application.commandhandling.command.SetInfoCommand
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.transfer.Input

@Singleton
class SetCommandFactory : SpecialCommandFactory() {
    override fun internalConstruct(input: Input) = input.command.let { cmd ->
        when {
            input.hasData() && cmd.size1() -> SetCommand(DEFAULT, input)
            input.hasNoData() && cmd.size2() && cmd.isInfoVariant() -> SetInfoCommand()
            input.hasData() && cmd.size2() && cmd.isSlotted() -> SetCommand(cmd.getSlot(), input)
            else -> null
        }
    }
}
