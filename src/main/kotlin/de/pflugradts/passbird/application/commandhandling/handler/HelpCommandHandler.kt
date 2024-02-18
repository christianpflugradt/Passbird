package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.HelpCommand

class HelpCommandHandler @Inject constructor(
    @Inject private val p: CanPrintInfo,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleHelpCommand(@Suppress("UNUSED_PARAMETER") helpCommand: HelpCommand) {
        userInterfaceAdapterPort.send(
            p.outBold("\nUsage: [command][parameter]\n"),
            p.out("A command takes at most one parameter which is usually an EggId.\n\n"),
            p.outBold("Commands:\n\n"),
            p.outBold("\tg[EggId]"), p.out(" (get) copies the Password contained in that Egg to clipboard\n"),
            p.outBold("\ts[EggId]"), p.out(" (set) sets a random Password for this Egg overwriting any that existed\n"),
            p.outBold("\tc[EggId]"), p.out(" (custom set) like set but prompts the user to input a new Password\n"),
            p.outBold("\tv[EggId]"), p.out(" (view) prints the Password contained in that Egg to the console\n"),
            p.outBold("\tr[EggId]"), p.out(" (rename) renames an Egg by prompting the user for a new one\n"),
            p.outBold("\td[EggId]"), p.out(" (discard) removes the Egg entirely from the Tree\n"),
            p.out("\n"),
            p.outBold("\te"), p.out(" (export) exports the Password Tree in a human readable json format\n"),
            p.outBold("\ti"), p.out(" (import) imports Passwords into the Tree from a json file\n"),
            p.outBold("\tl"), p.out(" (list) non parameterized, lists all Eggs in the current Nest\n"),
            p.outBold("\th"), p.out(" (help) prints this help\n"),
            p.outBold("\tq"), p.out(" (quit) terminates Passbird application \n"),
            p.out("\n"),
            p.outBold("\tn"), p.out(" (Nests) view available Nests and print Nest specific help\n"),
            p.outBold("\ts?"), p.out(" (Password configurations) view available configurations and print set specific help\n"),
        )
    }
}
