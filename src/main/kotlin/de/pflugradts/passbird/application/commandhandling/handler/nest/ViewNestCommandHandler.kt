package de.pflugradts.passbird.application.commandhandling.handler.nest

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.ViewNestCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.NestService

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
Current namespace: $currentNest
Available namespaces:
$availableNests
Available namespace commands:
${'\t'}n (view) displays current namespace, available namespaces and namespace commands
${'\t'}n0 (switch to default) switches to the default namespace
${'\t'}n[1-9] (switch) switches to the namespace at the given slot (between 1 and 9 inclusively)
${'\t'}n[1-9][key] (assign) assigns the password for that key to the specified namespace
${'\t'}n+[1-9] (create) creates a new namespace at the specified slot
${'\t'}[NOT YET IMPLEMENTED] n-[1-9] (discard) discards the namespace at the specified slot
"""
                        .let { "\n$it\n" },
                ),
            ),
        )
        userInterfaceAdapterPort.sendLineBreak()
    }

    private val currentNest get() = nestService.getCurrentNest().shell.asString()
    private val availableNests get() = getAvailableNests(includeCurrent = true).let {
        if (hasCustomNests()) it else "$it\t(use the n+ command to create custom namespaces)\n"
    }
}
