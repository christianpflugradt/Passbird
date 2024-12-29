package de.pflugradts.passbird.application.commandhandling.handler.protein

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.ProteinInfoCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler

class ProteinInfoCommandHandler @Inject constructor(
    private val canPrintInfo: CanPrintInfo,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleProteinInfoCommand(@Suppress("UNUSED_PARAMETER") proteinInfoCommand: ProteinInfoCommand) {
        with(canPrintInfo) {
            userInterfaceAdapterPort.send(
                outBold("\nAvailable Protein commands:\n"),
                outBold("\n\tp?"),
                out(" (help)                Displays this help menu for Protein commands."),
                outBold("\n\tp[EggId]"),
                out(" (info)          Displays the Protein Types associated with the specified Egg."),
                outBold("\n\tp*[EggId]"),
                out(" (details)      Displays both the Protein Types and their Structures for the specified Egg."),
                outBold("\n\tp[0-9][EggId]"),
                out(" (copy)     Copies the Protein Structure in the specified Slot (0–9) to the clipboard."),
                outBold("\n\tp+[0-9][EggId]"),
                out(" (update)  Updates the Protein Structure and optionally the Type in the specified Slot."),
                outBold("\n\tp-[0-9][EggId]"),
                out(" (discard) Deletes the Protein Structure and Type from the specified Slot."),
                out("\n"),
            )
        }
    }
}
