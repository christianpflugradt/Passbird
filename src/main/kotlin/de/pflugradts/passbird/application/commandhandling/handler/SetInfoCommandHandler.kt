package de.pflugradts.passbird.application.commandhandling.handler
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.SetInfoCommand
import de.pflugradts.passbird.application.commandhandling.handler.configValueLine
import de.pflugradts.passbird.application.configuration.ReadableConfiguration

class SetInfoCommandHandler constructor(
    private val canPrintInfo: CanPrintInfo,
    private val configuration: ReadableConfiguration,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : TypedCommandHandler<SetInfoCommand>(SetInfoCommand::class.java) {
    override fun handleCommand(@Suppress("UNUSED_PARAMETER") command: SetInfoCommand) {
        with(canPrintInfo) {
            printDefaultConfiguration()
            printCustomConfigurations()
            userInterfaceAdapterPort.send(*setCommandOutputs().toTypedArray())
        }
    }

    private fun CanPrintInfo.printDefaultConfiguration() = userInterfaceAdapterPort.send(
        outBold("\n0: Default\n"),
        out(configValueLine("length:", "${configuration.application.password.length} characters")),
        out(defaultSpecialCharactersLine()),
    )

    private fun defaultSpecialCharactersLine() = if (configuration.application.password.specialCharacters) {
        ""
    } else {
        "\n${configValueLine("special:", "no special characters")}"
    }

    private fun printCustomConfigurations() = configuration.application.password.customPasswordConfigurations
        .forEachIndexed(::printCustomConfiguration)

    private fun printCustomConfiguration(index: Int, config: ReadableConfiguration.CustomPasswordConfiguration) {
        userInterfaceAdapterPort.send(
            canPrintInfo.outBold("${index + 1}: ${config.name}\n"),
            canPrintInfo.out(configValueLine("length:", "${config.length} characters")),
        )
        appendCharacterRules(config)
        appendSpecialCharacterRule(config)
    }

    private fun appendCharacterRules(config: ReadableConfiguration.CustomPasswordConfiguration) {
        if (!config.hasNumbers) userInterfaceAdapterPort.send(canPrintInfo.out(configValueLine("numbers:", "no numbers")))
        if (!config.hasLowercaseLetters && !config.hasUppercaseLetters) {
            userInterfaceAdapterPort.send(canPrintInfo.out(configValueLine("letters:", "no letters")))
        } else if (!config.hasLowercaseLetters) {
            userInterfaceAdapterPort.send(canPrintInfo.out(configValueLine("lowercase:", "no lowercase letters")))
        } else if (!config.hasUppercaseLetters) {
            userInterfaceAdapterPort.send(canPrintInfo.out(configValueLine("uppercase:", "no uppercase letters")))
        }
    }

    private fun appendSpecialCharacterRule(config: ReadableConfiguration.CustomPasswordConfiguration) {
        if (!config.hasSpecialCharacters) {
            userInterfaceAdapterPort.send(canPrintInfo.out(configValueLine("special:", "no special characters")))
        } else if (config.unusedSpecialCharacters.isNotEmpty()) {
            userInterfaceAdapterPort.send(
                canPrintInfo.out(configValueLine("special:", "unused special characters: ${config.unusedSpecialCharacters}")),
            )
        }
    }

    private fun CanPrintInfo.setCommandOutputs() = buildList {
        add(outBold("\nAvailable Set commands:\n"))
        addAll(commandInfoOutputs("s?", "(help)", "Displays an overview of available password configurations.\n", commandColumnWidth = 15))
        addAll(
            commandInfoOutputs(
                "s[EggId]",
                "(set)",
                "Sets a random password for the specified EggId using the default configuration.\n",
                commandColumnWidth = 15,
            ),
        )
        addAll(
            commandInfoOutputs(
                "s*[EggId]",
                "(set once)",
                "Sets a random password for the specified EggId using a one-time configuration.\n",
                commandColumnWidth = 15,
            ),
        )
        addAll(
            commandInfoOutputs(
                "s[1-9][EggId]",
                "(set custom)",
                "Sets a random password for the specified EggId using a custom configuration.\n",
                commandColumnWidth = 15,
            ),
        )
    }
}
