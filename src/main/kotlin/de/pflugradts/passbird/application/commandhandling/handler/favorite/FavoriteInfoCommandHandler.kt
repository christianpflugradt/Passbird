package de.pflugradts.passbird.application.commandhandling.handler.favorite
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.FavoriteInfoCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.commandInfoOutputs
class FavoriteInfoCommandHandler constructor(
    private val canPrintInfo: CanPrintInfo,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : TypedCommandHandler<FavoriteInfoCommand>(FavoriteInfoCommand::class.java) {
    override fun handleCommand(@Suppress("UNUSED_PARAMETER") command: FavoriteInfoCommand) {
        with(canPrintInfo) {
            userInterfaceAdapterPort.send(*favoriteCommandOutputs().toTypedArray())
        }
    }

    private fun CanPrintInfo.favoriteCommandOutputs() = buildList {
        add(outBold("\nAvailable Favorite commands:\n"))
        addAll(
            commandInfoOutputs(
                "f?",
                "(help)",
                "Displays this help menu for Favorite commands.",
                commandColumnWidth = 15,
                actionColumnWidth = 13,
            ),
        )
        addAll(
            commandInfoOutputs(
                "f",
                "(info)",
                "Lists the favorite EggIds for the current Nest.",
                commandColumnWidth = 15,
                actionColumnWidth = 13,
            ),
        )
        addAll(
            commandInfoOutputs(
                "f[0-9]",
                "(copy)",
                "Copies the EggId from the specified Favorite Slot to the clipboard.",
                commandColumnWidth = 15,
                actionColumnWidth = 13,
            ),
        )
        addAll(
            commandInfoOutputs(
                "f[0-9]Command",
                "(use)",
                "Executes the specified command using the EggId from the given Favorite Slot.",
                commandColumnWidth = 15,
                actionColumnWidth = 13,
            ),
        )
        addAll(
            commandInfoOutputs(
                "f+[0-9][EggId]",
                "(assign)",
                "Assigns the specified EggId to the given Favorite Slot.",
                commandColumnWidth = 15,
                actionColumnWidth = 13,
            ),
        )
        addAll(commandInfoOutputs("f-[0-9]", null, "Clears the specified Favorite Slot.", commandColumnWidth = 15, actionColumnWidth = 13))
        add(out("\n"))
    }
}
