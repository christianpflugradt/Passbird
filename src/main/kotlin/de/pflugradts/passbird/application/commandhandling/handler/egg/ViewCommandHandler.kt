package de.pflugradts.passbird.application.commandhandling.handler.egg

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.ViewCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService

class ViewCommandHandler @Inject constructor(
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleViewCommand(viewCommand: ViewCommand) {
        passwordService.viewPassword(viewCommand.argument).ifPresent { userInterfaceAdapterPort.send(outputOf(it)) }
        viewCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
