package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.SetInfoCommand
import de.pflugradts.passbird.application.configuration.ReadableConfiguration

class SetInfoCommandHandler @Inject constructor(
    private val canPrintInfo: CanPrintInfo,
    private val configuration: ReadableConfiguration,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleSetInfoCommand(@Suppress("UNUSED_PARAMETER") setInfoCommand: SetInfoCommand) {
        with(canPrintInfo) {
            userInterfaceAdapterPort.send(
                outBold("\n0: Default\n"),
                out("\t${configuration.application.password.length} characters"),
                out(if (configuration.application.password.specialCharacters) "" else "\n\tno special characters"),
            )
            configuration.application.password.customPasswordConfigurations.forEachIndexed { index, it ->
                userInterfaceAdapterPort.send(
                    outBold("${index + 1}: ${it.name}\n"),
                    out("\t${it.length} characters"),
                )
                if (!it.hasNumbers) userInterfaceAdapterPort.send(out("\tno numbers"))
                if (!it.hasLowercaseLetters && !it.hasUppercaseLetters) {
                    userInterfaceAdapterPort.send(out("\tno letters"))
                } else if (!it.hasLowercaseLetters) {
                    userInterfaceAdapterPort.send(out("\tno lowercase letters"))
                } else if (!it.hasUppercaseLetters) {
                    userInterfaceAdapterPort.send(out("\tno uppercase letters"))
                }
                if (!it.hasSpecialCharacters) {
                    userInterfaceAdapterPort.send(out("\tno special characters"))
                } else if (it.unusedSpecialCharacters.isNotEmpty()) {
                    userInterfaceAdapterPort.send(out("\tunused special characters: ${it.unusedSpecialCharacters}"))
                }
            }
            userInterfaceAdapterPort.send(
                outBold("\nAvailable Set commands:\n"),
                outBold("\n\ts?"),
                out(" (help)                  Displays an overview of available password configurations.\n"),
                outBold("\n\ts[EggId]"),
                out(" (set)             Sets a random password for the specified EggId using the default configuration."),
                outBold("\n\ts[1-9][EggId]"),
                out(" (set custom) Sets a random password for the specified EggId using a custom configuration."),
            )
        }
    }
}
