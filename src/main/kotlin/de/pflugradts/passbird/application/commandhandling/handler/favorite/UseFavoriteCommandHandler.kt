package de.pflugradts.passbird.application.commandhandling.handler.favorite

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.InputHandler
import de.pflugradts.passbird.application.commandhandling.command.UseFavoriteCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService
import jakarta.inject.Inject

class UseFavoriteCommandHandler @Inject constructor(
    private val inputHandler: InputHandler,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleUseFavoriteCommand(useFavoriteCommand: UseFavoriteCommand) {
        passwordService.viewFavoriteEntry(useFavoriteCommand.slot).ifPresentOrElse(
            block = { inputHandler.handleInput(inputOf(useFavoriteCommand.argument + it)) },
            other = {
                userInterfaceAdapterPort.send(
                    outputOf(shellOf("Favorite entry at slot ${useFavoriteCommand.slot.index()} does not exist.")),
                )
            },
        )
        useFavoriteCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
