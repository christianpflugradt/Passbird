package de.pflugradts.passbird.application.commandhandling.handler.favorite
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.ViewFavoriteCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService
class ViewFavoriteCommandHandler constructor(
    private val canPrintInfo: CanPrintInfo,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : TypedCommandHandler<ViewFavoriteCommand>(ViewFavoriteCommand::class.java) {
    override fun handleCommand(@Suppress("UNUSED_PARAMETER") command: ViewFavoriteCommand) {
        val favorites = passwordService.viewFavorites()
        try {
            userInterfaceAdapterPort.sendLineBreak()
            with(canPrintInfo) {
                if (favorites.any { it.isPresent }) {
                    favorites.forEachIndexed { index, favorite ->
                        favorite.ifPresent {
                            userInterfaceAdapterPort.send(outBold("$index:"), out(" ${it.asString()}"))
                        }
                    }
                } else {
                    userInterfaceAdapterPort.send(outputOf(shellOf("Favorite EggIds are empty.")))
                }
            }
            userInterfaceAdapterPort.sendLineBreak()
        } finally {
            favorites.forEach { it.ifPresent(Shell::scramble) }
        }
    }
}
