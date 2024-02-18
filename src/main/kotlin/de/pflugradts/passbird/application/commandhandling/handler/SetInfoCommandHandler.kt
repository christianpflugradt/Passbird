package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.SetInfoCommand
import de.pflugradts.passbird.application.configuration.ReadableConfiguration

class SetInfoCommandHandler @Inject constructor(
    @Inject private val p: CanPrintInfo,
    @Inject private val configuration: ReadableConfiguration,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleSetInfoCommand(@Suppress("UNUSED_PARAMETER") setInfoCommand: SetInfoCommand) {
        userInterfaceAdapterPort.send(
            p.outBold("\n0: Default\n"),
            p.out("\t${configuration.application.password.length} characters"),
            p.out(if (configuration.application.password.specialCharacters) "" else "\n\tno special characters"),
        )
        configuration.application.password.customPasswordConfigurations.forEachIndexed { index, it ->
            userInterfaceAdapterPort.send(
                p.outBold("${index + 1}: ${it.name}\n"),
                p.out("\t${it.length} characters"),
            )
            if (!it.hasNumbers) userInterfaceAdapterPort.send(p.out("\tno numbers"))
            if (!it.hasLowercaseLetters && !it.hasUppercaseLetters) {
                userInterfaceAdapterPort.send(p.out("\tno letters"))
            } else if (!it.hasLowercaseLetters) {
                userInterfaceAdapterPort.send(p.out("\tno lowercase letters"))
            } else if (!it.hasUppercaseLetters) {
                userInterfaceAdapterPort.send(p.out("\tno uppercase letters"))
            }
            if (!it.hasSpecialCharacters) {
                userInterfaceAdapterPort.send(p.out("\tno special characters"))
            } else if (it.unusedSpecialCharacters.isNotEmpty()) {
                userInterfaceAdapterPort.send(p.out("\tunused special characters: ${it.unusedSpecialCharacters}"))
            }
        }
        userInterfaceAdapterPort.send(
            p.outBold("\nAvailable Set commands:\n"),
            p.outBold("\n\ts[EggId] "),
            p.out("sets a random Password with default configuration"),
            p.outBold("\n\ts[1-9][EggId] "),
            p.out("sets a random Password with specified configuration"),
            p.outBold("\n\ts? "),
            p.out("prints this overview\n"),
        )
    }
}
