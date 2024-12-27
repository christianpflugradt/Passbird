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
                outBold("\n\nAvailable Memory commands:\n"),
                outBold("\n\tm?"), out(" (help) prints this help"),
                outBold("\n\tm"), out(" (info) prints the EggIdMemory"),
                outBold("\n\tm[0-9]"), out(" (copy) copies the memorized EggId to clipboard"),
                outBold("\n\tm[0-9]Command"), out(" (use) invokes the specified command with the memorized EggId"),
                out("\n"),
            )
        }
    }
}
