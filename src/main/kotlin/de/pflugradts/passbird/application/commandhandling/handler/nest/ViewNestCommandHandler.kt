package de.pflugradts.passbird.application.commandhandling.handler.nest

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanListAvailableNests
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.ViewNestCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.service.nest.NestService
import jakarta.inject.Inject

class ViewNestCommandHandler @Inject constructor(
    private val canPrintInfo: CanPrintInfo,
    private val canListAvailableNests: CanListAvailableNests,
    private val nestService: NestService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleViewNestCommand(@Suppress("UNUSED_PARAMETER") viewNestCommand: ViewNestCommand) {
        with(canPrintInfo) {
            userInterfaceAdapterPort.send(
                outBold("\nCurrent Nest: "), out(currentNest),
                outBold("\n\nAvailable Nests:\n"),
                out(availableNests),
                outBold("\n\nAvailable Nest commands:\n"),
                outBold("\n\tn"), out(" (view)               Displays the current Nest, available Nests, and related commands."),
                outBold("\n\tn0"), out(" (switch)            Switches to the default Nest."),
                outBold("\n\tn[1-9]"), out(" (switch)        Switches to the Nest in the specified Nest Slot (1–9)."),
                outBold("\n\tn[EggId]"), out(" (assign) Assigns the specified EggId to a Nest selected interactively."),
                outBold("\n\tn+[1-9]"), out(" (create)       Creates a new Nest in the specified Nest Slot."),
                outBold("\n\tn-[1-9]"), out(" (discard)      Deletes the Nest in the specified Nest Slot."),
                out("\n"),
            )
        }
    }

    private val currentNest get() = nestService.currentNest().viewNestId().asString()
    private val availableNests get() = canListAvailableNests.getAvailableNests(includeCurrent = true).let {
        if (canListAvailableNests.hasCustomNests()) it else "$it\t(use the n+ command to create custom Nests)\n"
    }
}
