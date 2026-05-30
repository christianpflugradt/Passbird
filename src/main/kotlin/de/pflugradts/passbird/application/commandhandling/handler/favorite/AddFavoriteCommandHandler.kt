package de.pflugradts.passbird.application.commandhandling.handler.favorite
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.AddFavoriteCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.domain.service.password.PasswordService
class AddFavoriteCommandHandler constructor(
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<AddFavoriteCommand>(AddFavoriteCommand::class.java) {
    override fun handleCommand(command: AddFavoriteCommand) {
        if (passwordService.putFavorite(command.slot, command.argument).failure) {
            commandExecutionTracker.markFailure()
        }
        command.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
