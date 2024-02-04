package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.SetInfoCommand
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting

class SetInfoCommandHandler @Inject constructor(
    @Inject private val configuration: ReadableConfiguration,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleSetInfoCommand(@Suppress("UNUSED_PARAMETER") setInfoCommand: SetInfoCommand) {
        userInterfaceAdapterPort.send(
            outBold("\n0: Default\n"),
            out("\t${configuration.application.password.length} characters\n"),
            out(if (configuration.application.password.specialCharacters) "" else "\tno special characters"),
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
            userInterfaceAdapterPort.send(
                outBold("\nAvailable Set Commands:\n"),
                outBold("\n\ts[EggId] "),
                out("sets a random Password with default configuration"),
                outBold("\n\ts[1-9][EggId] "),
                out("sets a random Password with specified configuration"),
                outBold("\n\ts? "),
                out("prints this overview\n"),
            )
        }
    }
}

private fun outBold(text: String) = Output.outputOf(Shell.shellOf(text), OutputFormatting.HIGHLIGHT)
private fun out(text: String) = Output.outputOf(Shell.shellOf(text), OutputFormatting.DEFAULT)
