package de.pflugradts.passbird.application.commandhandling.handler.favorite

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.FavoriteInfoCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import jakarta.inject.Inject

class FavoriteInfoCommandHandler @Inject constructor(
    private val canPrintInfo: CanPrintInfo,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleFavoriteInfoCommand(@Suppress("UNUSED_PARAMETER") favoriteInfoCommand: FavoriteInfoCommand) {
        with(canPrintInfo) {
            userInterfaceAdapterPort.send(
                outBold("\nAvailable Favorite commands:\n"),
                outBold("\n\tf?"), out(" (help)            Displays this help menu for Favorite commands."),
                outBold("\n\tf"), out(" (info)             Lists the favorite EggIds for the current Nest."),
                outBold("\n\tf[0-9]"), out(" (copy)        Copies the EggId from the specified Favorite Slot to the clipboard."),
                outBold("\n\tf[0-9]Command"), out(" (use)  Executes the specified command using the EggId from the given Favorite Slot."),
                outBold("\n\tf+[0-9][EggId]"), out("       Assigns the specified EggId to the given Favorite Slot."),
                outBold("\n\tf-[0-9]"), out("            Clears the specified Favorite Slot."),
                out("\n"),
            )
        }
    }
}
