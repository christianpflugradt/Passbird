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
            userInterfaceAdapterPort.send(
                *buildList {
                    add(outBold("\nAvailable Favorite commands:\n"))
                    addAll(commandInfoOutputs("f?", " (help)            Displays this help menu for Favorite commands.").toList())
                    addAll(commandInfoOutputs("f", " (info)            Lists the favorite EggIds for the current Nest.").toList())
                    addAll(
                        commandInfoOutputs(
                            "f[0-9]",
                            " (copy)            Copies the EggId from the specified Favorite Slot to the clipboard.",
                        ).toList(),
                    )
                    addAll(
                        commandInfoOutputs(
                            "f[0-9]Command",
                            " (use)             Executes the specified command using the EggId from the given Favorite Slot.",
                        ).toList(),
                    )
                    addAll(
                        commandInfoOutputs(
                            "f+[0-9][EggId]",
                            "                   Assigns the specified EggId to the given Favorite Slot.",
                        ).toList(),
                    )
                    addAll(commandInfoOutputs("f-[0-9]", "                   Clears the specified Favorite Slot.").toList())
                    add(out("\n"))
                }.toTypedArray(),
            )
        }
    }
}
