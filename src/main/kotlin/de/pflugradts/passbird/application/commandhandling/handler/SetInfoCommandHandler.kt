package de.pflugradts.passbird.application.commandhandling.handler
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.SetInfoCommand
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
class SetInfoCommandHandler constructor(
    private val canPrintInfo: CanPrintInfo,
    private val configuration: ReadableConfiguration,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : TypedCommandHandler<SetInfoCommand>(SetInfoCommand::class.java) {
    override fun handleCommand(@Suppress("UNUSED_PARAMETER") command: SetInfoCommand) {
        with(canPrintInfo) {
            userInterfaceAdapterPort.send(
                outBold("\n0: Default\n"),
                out("  ${configuration.application.password.length} characters"),
                out(if (configuration.application.password.specialCharacters) "" else "\n  no special characters"),
            )
            configuration.application.password.customPasswordConfigurations.forEachIndexed { index, it ->
                userInterfaceAdapterPort.send(
                    outBold("${index + 1}: ${it.name}\n"),
                    out("  ${it.length} characters"),
                )
                if (!it.hasNumbers) userInterfaceAdapterPort.send(out("  no numbers"))
                if (!it.hasLowercaseLetters && !it.hasUppercaseLetters) {
                    userInterfaceAdapterPort.send(out("  no letters"))
                } else if (!it.hasLowercaseLetters) {
                    userInterfaceAdapterPort.send(out("  no lowercase letters"))
                } else if (!it.hasUppercaseLetters) {
                    userInterfaceAdapterPort.send(out("  no uppercase letters"))
                }
                if (!it.hasSpecialCharacters) {
                    userInterfaceAdapterPort.send(out("  no special characters"))
                } else if (it.unusedSpecialCharacters.isNotEmpty()) {
                    userInterfaceAdapterPort.send(out("  unused special characters: ${it.unusedSpecialCharacters}"))
                }
            }
            userInterfaceAdapterPort.send(
                *buildList {
                    add(outBold("\nAvailable Set commands:\n"))
                    addAll(commandInfoOutputs("s?", " (help)        Displays an overview of available password configurations.\n").toList())
                    addAll(
                        commandInfoOutputs(
                            "s[EggId]",
                            " (set)         Sets a random password for the specified EggId using the default configuration.\n",
                        ).toList(),
                    )
                    addAll(
                        commandInfoOutputs(
                            "s*[EggId]",
                            " (set once)    Sets a random password for the specified EggId using a one-time configuration.\n",
                        ).toList(),
                    )
                    addAll(
                        commandInfoOutputs(
                            "s[1-9][EggId]",
                            " (set custom)  Sets a random password for the specified EggId using a custom configuration.\n",
                        ).toList(),
                    )
                }.toTypedArray(),
            )
        }
    }
}
