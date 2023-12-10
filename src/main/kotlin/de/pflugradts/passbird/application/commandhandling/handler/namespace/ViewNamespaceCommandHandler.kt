package de.pflugradts.passbird.application.commandhandling.handler.namespace

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.ViewNamespaceCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.NamespaceService

class ViewNamespaceCommandHandler @Inject constructor(
    @Inject private val namespaceService: NamespaceService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler, CanListAvailableNamespaces(namespaceService) {
    @Subscribe
    private fun handleViewNamespaceCommand(@Suppress("UNUSED_PARAMETER") viewNamespaceCommand: ViewNamespaceCommand) {
        userInterfaceAdapterPort.send(
            outputOf(
                bytesOf(
                    """
Current namespace: $currentNamespace
Available namespaces:
$availableNamespaces
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

    private val currentNamespace get() = namespaceService.getCurrentNamespace().bytes.asString()
    private val availableNamespaces get() = getAvailableNamespaces(includeCurrent = true).let {
        if (hasCustomNamespaces()) it else "$it\t(use the n+ command to create custom namespaces)\n"
    }
}
