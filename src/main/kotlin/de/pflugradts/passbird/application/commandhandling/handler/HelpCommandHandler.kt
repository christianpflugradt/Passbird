package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.HelpCommand
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf

private const val TAB = "\t"

class HelpCommandHandler @Inject constructor(
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleHelpCommand(@Suppress("UNUSED_PARAMETER") helpCommand: HelpCommand) {
        userInterfaceAdapterPort.send(
            outputOf(
                bytesOf(
                    """
                    Usage: [command][parameter]
                    A command takes at most one parameter which is either
                    a key to a password or an absolute path to a file.

                    commands:
                    ${TAB}g[key] (get) copies the password for that key to clipboard
                    ${TAB}s[key] (set) sets a random password for a key overwriting any that existed
                    ${TAB}c[key] (custom set) like set but prompts the user to input a new password
                    ${TAB}v[key] (view) prints the password for that key to the console
                    ${TAB}r[key] (rename) renames a key by prompting the user for a new one
                    ${TAB}d[key] (discard) removes key and password from the database
                    ${TAB}e[directory] (export) exports the password database as a human readable json file to the specified directory
                    ${TAB}i[directory] (import) imports a json file containing passwords into the database from the specified directory
                    ${TAB}l (list) non parameterized, lists all keys in the database
                    ${TAB}n (namespaces) view available namespaces and print namespace specific help
                    ${TAB}h (help) non parameterized, prints this help
                    ${TAB}q (quit) quits pwman3 applicationImpl

                    """.trimIndent(),
                ),
            ),
        )
        userInterfaceAdapterPort.sendLineBreak()
    }
}
