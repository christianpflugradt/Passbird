package de.pflugradts.passbird.application.commandhandling.handler.memory
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.MemoryInfoCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.commandInfoOutputs
class MemoryInfoCommandHandler constructor(
    private val canPrintInfo: CanPrintInfo,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : TypedCommandHandler<MemoryInfoCommand>(MemoryInfoCommand::class.java) {
    override fun handleCommand(@Suppress("UNUSED_PARAMETER") command: MemoryInfoCommand) {
        with(canPrintInfo) {
            userInterfaceAdapterPort.send(
                *buildList {
                    add(outBold("\nAvailable Memory commands:\n"))
                    addAll(commandInfoOutputs("m?", " (help)           Displays this help menu for Memory commands.").toList())
                    addAll(commandInfoOutputs("m", " (info)           Lists the EggIds currently stored in the EggIdMemory.").toList())
                    addAll(
                        commandInfoOutputs(
                            "m[0-9]",
                            " (copy)           Copies the EggId from the specified Memory Slot to the clipboard.",
                        ).toList(),
                    )
                    addAll(
                        commandInfoOutputs(
                            "m[0-9]Command",
                            " (use)            Executes the specified command using the EggId from the given Memory Slot.",
                        ).toList(),
                    )
                    add(out("\n"))
                }.toTypedArray(),
            )
        }
    }
}
