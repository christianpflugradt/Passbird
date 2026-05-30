package de.pflugradts.passbird.application.commandhandling.handler.favorite

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.InputHandler
import de.pflugradts.passbird.application.commandhandling.command.UseFavoriteCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.application.useScrambled
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService

class UseFavoriteCommandHandler(
    private val inputHandler: () -> InputHandler,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<UseFavoriteCommand>(UseFavoriteCommand::class.java) {
    override fun handleCommand(command: UseFavoriteCommand) {
        var delegated = false
        passwordService.viewFavoriteEntry(command.slot).ifPresentOrElse(
            block = { favorite ->
                favorite.useScrambled {
                    delegated = true
                    inputHandler().handleInput(inputOf(command.argument + it))
                    commandExecutionTracker.mark(commandExecutionTracker.lastCompletedOutcome())
                }
            },
            other = {
                commandExecutionTracker.markFailure()
                userInterfaceAdapterPort.send(
                    outputOf(shellOf("Favorite entry at slot ${command.slot.index()} does not exist.")),
                )
            },
        )
        command.invalidateInput()
        if (!delegated) userInterfaceAdapterPort.sendLineBreak()
    }
}
