package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.ViewCommand
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService

class ViewCommandHandler @Inject constructor(
    @Inject private val passwordService: PasswordService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleViewCommand(viewCommand: ViewCommand) {
        // below line results in an arch unit violation due to https://github.com/TNG/ArchUnit/issues/981
        // passwordService.viewPassword(viewCommand.argument).ifPresent { userInterfaceAdapterPort.send(outputOf(it)) }
        passwordService.viewPassword(viewCommand.argument).orNull()?.also { userInterfaceAdapterPort.send(outputOf(it)) }
        viewCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
