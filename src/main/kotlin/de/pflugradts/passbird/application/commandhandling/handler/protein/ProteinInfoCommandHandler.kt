package de.pflugradts.passbird.application.commandhandling.handler.protein
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.ProteinInfoCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.commandInfoOutputs
class ProteinInfoCommandHandler constructor(
    private val canPrintInfo: CanPrintInfo,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : TypedCommandHandler<ProteinInfoCommand>(ProteinInfoCommand::class.java) {
    override fun handleCommand(@Suppress("UNUSED_PARAMETER") command: ProteinInfoCommand) {
        with(canPrintInfo) {
            userInterfaceAdapterPort.send(
                *buildList {
                    add(outBold("\nAvailable Protein commands:\n"))
                    addAll(commandInfoOutputs("p?", " (help)          Displays this help menu for Protein commands.").toList())
                    addAll(
                        commandInfoOutputs(
                            "p[EggId]",
                            " (info)          Displays the Protein Types associated with the specified Egg.",
                        ).toList(),
                    )
                    addAll(
                        commandInfoOutputs(
                            "p*[EggId]",
                            " (details)       Displays both the Protein Types and their Structures for the " +
                                "specified Egg.",
                        ).toList(),
                    )
                    addAll(
                        commandInfoOutputs(
                            "p[0-9][EggId]",
                            " (copy)          Copies the Protein Structure in the specified Slot (0–9) to the clipboard.",
                        ).toList(),
                    )
                    addAll(
                        commandInfoOutputs(
                            "p+[EggId]",
                            " (guided)        Guides through creating Proteins for the specified Egg.",
                        ).toList(),
                    )
                    addAll(
                        commandInfoOutputs(
                            "p+[0-9][EggId]",
                            " (update)        Updates the Protein Structure and optionally the Type in the specified Slot.",
                        ).toList(),
                    )
                    addAll(
                        commandInfoOutputs(
                            "p-[0-9][EggId]",
                            " (discard)       Deletes the Protein Structure and Type from the specified Slot.",
                        ).toList(),
                    )
                    add(out("\n"))
                }.toTypedArray(),
            )
        }
    }
}
