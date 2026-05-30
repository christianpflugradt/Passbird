package de.pflugradts.passbird.application.commandhandling.handler.favorite
import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.AddFavoriteCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.service.password.PasswordService
class AddFavoriteCommandHandler constructor(
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : CommandHandler {
    @Subscribe
    private fun handleAddFavoriteCommand(addFavoriteCommand: AddFavoriteCommand) {
        if (passwordService.putFavorite(addFavoriteCommand.slot, addFavoriteCommand.argument).failure) {
            commandExecutionTracker.markFailure()
        }
        addFavoriteCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
