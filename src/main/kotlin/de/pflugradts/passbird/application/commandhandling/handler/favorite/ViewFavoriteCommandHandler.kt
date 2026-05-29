package de.pflugradts.passbird.application.commandhandling.handler.favorite

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.ViewFavoriteCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService
import jakarta.inject.Inject

class ViewFavoriteCommandHandler @Inject constructor(
    private val canPrintInfo: CanPrintInfo,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleViewFavoriteCommand(@Suppress("UNUSED_PARAMETER") viewFavoriteCommand: ViewFavoriteCommand) {
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
