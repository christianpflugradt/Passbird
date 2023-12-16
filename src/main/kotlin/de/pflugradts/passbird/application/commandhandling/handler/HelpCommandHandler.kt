package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.HelpCommand
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf

class HelpCommandHandler @Inject constructor(
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleHelpCommand(@Suppress("UNUSED_PARAMETER") helpCommand: HelpCommand) {
        userInterfaceAdapterPort.send(
            outputOf(
                shellOf(
                    """
                    Usage: [command][parameter]
                    A command takes at most one parameter which is usually an EggId.

                    commands:
                    ${'\t'}g[EggId] (get) copies the Password contained in that Egg to clipboard
                    ${'\t'}s[EggId] (set) sets a random Password for this Egg overwriting any that existed
                    ${'\t'}c[EggId] (custom set) like set but prompts the user to input a new Password
                    ${'\t'}v[EggId] (view) prints the Password contained in that Egg to the console
                    ${'\t'}r[EggId] (rename) renames an Egg by prompting the user for a new one
                    ${'\t'}d[EggId] (discard) removes the Egg entirely from the database
                    ${'\t'}e[directory] (export) exports the Password database as a human readable json file to the specified directory
                    ${'\t'}i[directory] (import) imports a json file containing Passwords into the database from the specified directory
                    ${'\t'}l (list) non parameterized, lists all Eggs in the database
                    ${'\t'}n (Nests) view available Nests and print Nest specific help
                    ${'\t'}h (help) prints this help
                    ${'\t'}q (quit) terminates Passbird application 

                    """.trimIndent(),
                ),
            ),
        )
        userInterfaceAdapterPort.sendLineBreak()
    }
}
