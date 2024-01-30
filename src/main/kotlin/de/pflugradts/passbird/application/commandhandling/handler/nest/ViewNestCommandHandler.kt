package de.pflugradts.passbird.application.commandhandling.handler.nest

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.ViewNestCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.DEFAULT
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.HIGHLIGHT
import de.pflugradts.passbird.domain.service.nest.NestService

class ViewNestCommandHandler @Inject constructor(
    @Inject private val nestService: NestService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler, CanListAvailableNests(nestService) {
    @Subscribe
    private fun handleViewNestCommand(@Suppress("UNUSED_PARAMETER") viewNestCommand: ViewNestCommand) {
        userInterfaceAdapterPort.send(
            outBold("\nCurrent Nest: "), out(currentNest),
            outBold("\n\nAvailable Nests:\n"),
            out(availableNests),
            outBold("\n\nAvailable Nest commands:\n"),
            outBold("\n\tn"), out(" (view) displays current Nest, available Nests and Nest commands"),
            outBold("\n\tn0"), out(" (switch) switches to the default Nest"),
            outBold("\n\tn[1-9]"), out(" (switch) switches to the Nest at the given Nest Slot (1-9 inclusively)"),
            outBold("\n\tn[EggId]"), out(" (move) moves the specified Egg to another Nest"),
            outBold("\n\tn+[1-9]"), out(" (create) creates a new Nest at the specified Nest Slot"),
            outBold("\n\tn-[1-9]"), out(" (discard) discards the Nest at the specified Nest Slot"),
            out("\n"),
        )
    }

    private val currentNest get() = nestService.currentNest().viewNestId().asString()
    private val availableNests get() = getAvailableNests(includeCurrent = true).let {
        if (hasCustomNests()) it else "$it\t(use the n+ command to create custom Nests)\n"
    }
}

private fun outBold(text: String) = outputOf(shellOf(text), HIGHLIGHT)
private fun out(text: String) = outputOf(shellOf(text), DEFAULT)
