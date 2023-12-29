package de.pflugradts.passbird.application.commandhandling.handler.nest

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.ViewNestCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.nest.NestService

class ViewNestCommandHandler @Inject constructor(
    @Inject private val nestService: NestService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler, CanListAvailableNests(nestService) {
    @Subscribe
    private fun handleViewNestCommand(@Suppress("UNUSED_PARAMETER") viewNestCommand: ViewNestCommand) {
        userInterfaceAdapterPort.send(
            outputOf(
                shellOf(
                    """
Current Nest: $currentNest

Available Nests:
$availableNests

Available Nest commands:
${'\t'}n (view) displays current Nest, available Nests and Nest commands
${'\t'}n0 (switch to default) switches to the default Nest
${'\t'}n[1-9] (switch) switches to the Nest at the given Nest Slot (between 1 and 9 inclusively)
${'\t'}n[1-9][EggId] (assign) assigns the Egg to the specified Nest
${'\t'}n+[1-9] (create) creates a new Nest at the specified Nest Slot
${'\t'}n-[1-9] (discard) discards the Nest at the specified Nest Slot
""",
                ),
            ),
        )
    }

    private val currentNest get() = nestService.currentNest().shell.asString()
    private val availableNests get() = getAvailableNests(includeCurrent = true).let {
        if (hasCustomNests()) it else "$it\t(use the n+ command to create custom Nests)\n"
    }
}
