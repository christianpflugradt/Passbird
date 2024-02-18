package de.pflugradts.passbird.application.commandhandling

import com.google.inject.Singleton
import de.pflugradts.passbird.domain.model.transfer.Input

@Singleton
class ProteinCommandFactory : SpecialCommandFactory() {
    override fun internalConstruct(input: Input) = input.command.let { cmd ->
        when {
            input.hasData() && cmd.size1() -> null // ping
            input.hasData() && cmd.size2() && cmd.isShowAllVariant() -> null // p*ing
            input.hasData() && cmd.size2() && cmd.isSlotted() -> null // p0ing
            input.hasData() && cmd.size3() && cmd.isAddVariant() && cmd.isSlotted() -> null // p+0ing
            input.hasData() && cmd.size3() && cmd.isDiscardVariant() && cmd.isSlotted() -> null // p-0ing
            else -> null
        }
    }
}
