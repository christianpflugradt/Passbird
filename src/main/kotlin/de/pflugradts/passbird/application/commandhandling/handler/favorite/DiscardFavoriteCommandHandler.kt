package de.pflugradts.passbird.application.commandhandling.handler.favorite

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.DiscardFavoriteCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.service.password.PasswordService
import jakarta.inject.Inject

class DiscardFavoriteCommandHandler @Inject constructor(
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : CommandHandler {
    @Subscribe
    private fun handleDiscardFavoriteCommand(discardFavoriteCommand: DiscardFavoriteCommand) {
        if (passwordService.discardFavorite(discardFavoriteCommand.slot).failure) {
            commandExecutionTracker.markFailure()
        }
        userInterfaceAdapterPort.sendLineBreak()
    }
}
