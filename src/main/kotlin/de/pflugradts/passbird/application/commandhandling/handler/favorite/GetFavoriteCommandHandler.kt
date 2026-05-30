package de.pflugradts.passbird.application.commandhandling.handler.favorite
import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.GetFavoriteCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.application.useScrambled
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService
class GetFavoriteCommandHandler constructor(
    private val passwordService: PasswordService,
    private val clipboardAdapterPort: ClipboardAdapterPort,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : CommandHandler {
    @Subscribe
    private fun handleGetFavoriteCommand(getFavoriteCommand: GetFavoriteCommand) {
        passwordService.viewFavoriteEntry(getFavoriteCommand.slot).ifPresentOrElse(
            block = { favorite ->
                favorite.useScrambled {
                    val clipboardResult = clipboardAdapterPort.post(outputOf(it))
                    if (clipboardResult.failure) {
                        commandExecutionTracker.markFailure()
                    }
                    clipboardResult.onSuccess {
                        userInterfaceAdapterPort.send(outputOf(shellOf("EggId copied to clipboard.")))
                    }
                }
            },
            other = {
                commandExecutionTracker.markFailure()
                userInterfaceAdapterPort.send(
                    outputOf(shellOf("Favorite entry at slot ${getFavoriteCommand.slot.index()} does not exist.")),
                )
            },
        )
        userInterfaceAdapterPort.sendLineBreak()
    }
}
