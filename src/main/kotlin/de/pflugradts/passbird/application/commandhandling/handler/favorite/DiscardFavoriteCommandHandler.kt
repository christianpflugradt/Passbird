package de.pflugradts.passbird.application.commandhandling.handler.favorite
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.DiscardFavoriteCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.domain.service.password.PasswordService
class DiscardFavoriteCommandHandler constructor(
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<DiscardFavoriteCommand>(DiscardFavoriteCommand::class.java) {
    override fun handleCommand(command: DiscardFavoriteCommand) {
        if (passwordService.discardFavorite(command.slot).failure) {
            commandExecutionTracker.markFailure()
        }
        userInterfaceAdapterPort.sendLineBreak()
    }
}
