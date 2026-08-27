package de.pflugradts.passbird.application.commandhandling.handler.yolk

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.YolkInfoCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.commandInfoOutputs

class YolkInfoCommandHandler(
    private val canPrintInfo: CanPrintInfo,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : TypedCommandHandler<YolkInfoCommand>(YolkInfoCommand::class.java) {
    override fun handleCommand(@Suppress("UNUSED_PARAMETER") command: YolkInfoCommand) {
        with(canPrintInfo) {
            userInterfaceAdapterPort.send(
                *buildList {
                    add(outBold("\nAvailable Yolk commands:\n"))
                    addAll(commandInfoOutputs("y?", " (help)       Displays this help menu for Yolk commands.").toList())
                    addAll(commandInfoOutputs("y[EggId]", " (view)       Displays the current TOTP code for the specified Egg.").toList())
                    addAll(
                        commandInfoOutputs(
                            "y+[EggId]",
                            " (set)        Prompts for a TOTP secret or otpauth URI and stores it for the specified Egg.",
                        ).toList(),
                    )
                    addAll(commandInfoOutputs("y-[EggId]", " (discard)    Deletes the Yolk stored for the specified Egg.").toList())
                    add(out("\n"))
                }.toTypedArray(),
            )
        }
    }
}
