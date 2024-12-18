package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.HelpCommand

class HelpCommandHandler @Inject constructor(
    private val canPrintInfo: CanPrintInfo,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleHelpCommand(@Suppress("UNUSED_PARAMETER") helpCommand: HelpCommand) {
        with(canPrintInfo) {
            userInterfaceAdapterPort.send(
                outBold("\nUsage: [command][parameter]\n"),
                out("A command takes at most one parameter which is usually an EggId.\n\n"),
                outBold("Commands:\n\n"),
                outBold("\tg[EggId]"), out(" (get) copies the Password contained in that Egg to clipboard\n"),
                outBold("\ts[EggId]"), out(" (set) sets a random Password for this Egg overwriting any that existed\n"),
                outBold("\tc[EggId]"), out(" (custom set) like set but prompts the user to input a new Password\n"),
                outBold("\tv[EggId]"), out(" (view) prints the Password contained in that Egg to the console\n"),
                outBold("\tr[EggId]"), out(" (rename) renames an Egg by prompting the user for a new one\n"),
                outBold("\td[EggId]"), out(" (discard) removes the Egg entirely from the Tree\n"),
                out("\n"),
                outBold("\te"), out(" (export) exports the Password Tree in a human readable json format\n"),
                outBold("\ti"), out(" (import) imports Passwords into the Tree from a json file\n"),
                outBold("\tl"), out(" (list) non parameterized, lists all Eggs in the current Nest\n"),
                outBold("\th"), out(" (help) prints this help\n"),
                outBold("\tq"), out(" (quit) terminates Passbird application \n"),
                out("\n"),
                outBold("\tn"), out(" (Nests) prints available Nests and Nest specific help\n"),
                outBold("\tp?"), out(" (Proteins) prints protein usage information\n"),
                outBold("\ts?"), out(" (Password configurations) prints available configurations and set specific help\n"),
            )
        }
    }
}
