package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.ProteinInfoCommand

class ProteinInfoCommandHandler @Inject constructor(
    @Inject private val canPrintInfo: CanPrintInfo,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleProteinInfoCommand(@Suppress("UNUSED_PARAMETER") proteinInfoCommand: ProteinInfoCommand) {
        with(canPrintInfo) {
            userInterfaceAdapterPort.send(
                outBold("\n\nAvailable Protein commands:\n"),
                outBold("\n\tp?"), out(" (help) prints this help"),
                outBold("\n\tp[EggId]"), out(" (info) prints Protein Types for specified Egg"),
                outBold("\n\tp*[EggId]"), out(" (complete info) prints Protein Types and Structures for specified Egg"),
                outBold("\n\tp[0-9][EggId]"), out(" (copy) copies the Protein Structure to clipboard"),
                outBold("\n\tp+[1-9][EggId]"), out(" (update) updates the Protein Structure and optionally Type as well"),
                // TBD: outBold("\n\tp-[1-9][EggId]"), out(" (clear) clears the Protein Structure and optionally Type as well"),
                out("\n"),
            )
        }
    }
}
