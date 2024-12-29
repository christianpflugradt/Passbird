package de.pflugradts.passbird.application.commandhandling.handler.memory

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.MemoryInfoCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler

class MemoryInfoCommandHandler @Inject constructor(
    private val canPrintInfo: CanPrintInfo,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleMemoryInfoCommand(@Suppress("UNUSED_PARAMETER") memoryInfoCommand: MemoryInfoCommand) {
        with(canPrintInfo) {
            userInterfaceAdapterPort.send(
                outBold("\nAvailable Memory commands:\n"),
                outBold("\n\tm?"), out(" (help)           Displays this help menu for Memory commands."),
                outBold("\n\tm"), out(" (info)            Lists the EggIds currently stored in the EggIdMemory."),
                outBold("\n\tm[0-9]"), out(" (copy)       Copies the EggId from the specified Memory Slot to the clipboard."),
                outBold("\n\tm[0-9]Command"), out(" (use) Executes the specified command using the EggId from the given Memory Slot."),
                out("\n"),
            )
        }
    }
}
