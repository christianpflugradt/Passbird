package de.pflugradts.passbird.application.commandhandling.handler.nest

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanListAvailableNests
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.ViewNestCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.service.nest.NestService

class ViewNestCommandHandler @Inject constructor(
    @Inject private val p: CanPrintInfo,
    @Inject private val n: CanListAvailableNests,
    @Inject private val nestService: NestService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleViewNestCommand(@Suppress("UNUSED_PARAMETER") viewNestCommand: ViewNestCommand) {
        userInterfaceAdapterPort.send(
            p.outBold("\nCurrent Nest: "), p.out(currentNest),
            p.outBold("\n\nAvailable Nests:\n"),
            p.out(availableNests),
            p.outBold("\n\nAvailable Nest commands:\n"),
            p.outBold("\n\tn"), p.out(" (view) displays current Nest, available Nests and Nest commands"),
            p.outBold("\n\tn0"), p.out(" (switch) switches to the default Nest"),
            p.outBold("\n\tn[1-9]"), p.out(" (switch) switches to the Nest at the given Nest Slot (1-9 inclusively)"),
            p.outBold("\n\tn[EggId]"), p.out(" (move) moves the specified Egg to another Nest"),
            p.outBold("\n\tn+[1-9]"), p.out(" (create) creates a new Nest at the specified Nest Slot"),
            p.outBold("\n\tn-[1-9]"), p.out(" (discard) discards the Nest at the specified Nest Slot"),
            p.out("\n"),
        )
    }

    private val currentNest get() = nestService.currentNest().viewNestId().asString()
    private val availableNests get() = n.getAvailableNests(includeCurrent = true).let {
        if (n.hasCustomNests()) it else "$it\t(use the n+ command to create custom Nests)\n"
    }
}
