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
                outBold("\tg[EggId]"), out(" (get)        Copies the password for the specified Egg to the clipboard.\n"),
                outBold("\ts[EggId]"), out(" (set)        Sets a random password for the specified Egg, overwriting any existing one.\n"),
                outBold("\tc[EggId]"), out(" (custom set) Prompts the user to input a custom password for the specified Egg.\n"),
                outBold("\tv[EggId]"), out(" (view)       Displays the password for the specified Egg in the console.\n"),
                outBold("\tr[EggId]"), out(" (rename)     Renames the specified Egg by prompting the user for a new EggId.\n"),
                outBold("\td[EggId]"), out(" (discard)    Deletes the specified Egg and its associated password.\n"),
                out("\n"),
                outBold("\te"), out(" (export)            Exports the Password Tree to a human-readable JSON file.\n"),
                outBold("\ti"), out(" (import)            Imports passwords from a JSON file into the Password Tree.\n"),
                outBold("\tl"), out(" (list)              Lists all Eggs in the current Nest.\n"),
                outBold("\th"), out(" (help)              Displays this help menu.\n"),
                outBold("\tq"), out(" (quit)              Exits the Passbird application.\n"),
                out("\n"),
                outBold("\tn"), out(" (Nests)             Displays available Nests and related commands.\n"),
                outBold("\tp?"), out(" (Proteins)         Displays Protein-related usage information.\n"),
                outBold("\ts?"), out(" (Password configs) Displays available password configurations and related help.\n"),
            )
        }
    }
}
