package de.pflugradts.passbird.application.commandhandling.handler.yolk

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.YolkInfoCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler

class YolkInfoCommandHandler(
    private val canPrintInfo: CanPrintInfo,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : TypedCommandHandler<YolkInfoCommand>(YolkInfoCommand::class.java) {
    override fun handleCommand(@Suppress("UNUSED_PARAMETER") command: YolkInfoCommand) {
        with(canPrintInfo) {
            userInterfaceAdapterPort.send(
                outBold("\nAvailable Yolk commands:\n"),
                outBold("\n\ty?"),
                out(" (help)       Displays this help menu for Yolk commands."),
                outBold("\n\ty[EggId]"),
                out(" (view)       Displays the current TOTP code for the specified Egg."),
                outBold("\n\ty+[EggId]"),
                out(" (set)        Prompts for a TOTP secret or otpauth URI and stores it for the specified Egg."),
                outBold("\n\ty-[EggId]"),
                out(" (discard)    Deletes the Yolk stored for the specified Egg."),
                out("\n"),
            )
        }
    }
}
