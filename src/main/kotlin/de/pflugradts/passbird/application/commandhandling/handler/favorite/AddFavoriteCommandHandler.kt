package de.pflugradts.passbird.application.commandhandling.handler.favorite

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.AddFavoriteCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.service.password.PasswordService
import jakarta.inject.Inject

class AddFavoriteCommandHandler @Inject constructor(
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleAddFavoriteCommand(addFavoriteCommand: AddFavoriteCommand) {
        passwordService.putFavorite(addFavoriteCommand.slot, addFavoriteCommand.argument)
        addFavoriteCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
