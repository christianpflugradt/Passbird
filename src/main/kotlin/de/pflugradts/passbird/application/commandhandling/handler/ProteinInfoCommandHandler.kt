package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.ProteinInfoCommand

class ProteinInfoCommandHandler @Inject constructor(
    @Inject private val p: CanPrintInfo,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleProteinInfoCommand(@Suppress("UNUSED_PARAMETER") proteinInfoCommand: ProteinInfoCommand) {
        userInterfaceAdapterPort.send(
            p.outBold("\n\nAvailable Protein commands:\n"),
            p.outBold("\n\tp?"), p.out(" (help) prints this help"),
            p.outBold("\n\tp?[EggId]"), p.out(" (info) prints Protein Types for specified Egg"),
            p.outBold("\n\tp*[EggId]"), p.out(" (complete info) prints Protein Types and Structures for specified Egg"),
            p.outBold("\n\tp[0-9][EggId]"), p.out(" (copy) copies the Protein Structure to clipboard"),
            p.outBold("\n\tp+[1-9][EggId]"), p.out(" (update) updates the Protein Structure and optionally Type as well"),
            p.outBold("\n\tp-[1-9][EggId]"), p.out(" (clear) clears the Protein Structure and optionally Type as well"),
            p.out("\n\n(ALL THESE COMMANDS ARE YET TO BE IMPLEMENTED)"),
            p.out("\n"),
        )
    }
}
