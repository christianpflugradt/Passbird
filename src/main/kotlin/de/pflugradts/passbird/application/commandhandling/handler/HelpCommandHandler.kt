package de.pflugradts.passbird.application.commandhandling.handler
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.HelpCommand
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.NestStats
import de.pflugradts.passbird.domain.service.password.PasswordService

class HelpCommandHandler constructor(
    private val canPrintInfo: CanPrintInfo,
    private val nestService: NestService,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : TypedCommandHandler<HelpCommand>(HelpCommand::class.java) {
    override fun handleCommand(command: HelpCommand) {
        with(canPrintInfo) {
            if (command.showStats) {
                userInterfaceAdapterPort.send(*statsOutputs().toTypedArray())
            }
            userInterfaceAdapterPort.send(
                outBold(if (command.showStats) "Usage: [command][parameter]\n" else "\nUsage: [command][parameter]\n"),
                out("A command takes at most one parameter which is usually an EggId.\n\n"),
                outBold("Commands:\n\n"),
                outBold("\tg[EggId]"), out(" (get)        Copies the password for the specified Egg to the clipboard.\n"),
                outBold("\ts[EggId]"), out(" (set)        Sets a random password for the specified Egg, overwriting any existing one.\n"),
                outBold(
                    "\ts*[EggId]",
                ),
                out(" (set once)   Sets a random password for the specified EggId using a one-time configuration.\n"),
                outBold("\tc[EggId]"), out(" (custom set) Prompts the user to input a custom password for the specified Egg.\n"),
                outBold("\tv[EggId]"), out(" (view)       Displays the password for the specified Egg in the console.\n"),
                outBold("\tr[EggId]"), out(" (rename)     Renames the specified Egg by prompting the user for a new EggId.\n"),
                outBold("\td[EggId]"), out(" (discard)    Deletes the specified Egg and its associated password.\n"),
                out("\n"),
                outBold("\te"), out(" (export)            Exports the Password Tree to a human-readable JSON file.\n"),
                outBold("\te*"), out(" (selective export)  Exports selected Nests or all Nests except selected Nests.\n"),
                outBold("\ti"), out(" (import)            Imports passwords from a JSON file into the Password Tree.\n"),
                outBold("\ti*"), out(" (selective import)  Imports one Nest from a JSON file into a selected Nest Slot.\n"),
                outBold("\tk"), out(" (keystore)          Changes the master password of the keystore.\n"),
                outBold("\tl"), out(" (list)              Lists all EggIds in the current Nest.\n"),
                outBold("\tl[filter]"), out("              Lists EggIds in the current Nest whose name contains filter.\n"),
                outBold("\tl*"), out("                    Lists all EggIds across all Nests, grouped by Nest.\n"),
                outBold("\tl*[filter]"), out("             Lists EggIds across all Nests whose name contains filter, grouped by Nest.\n"),
                outBold("\t."), out(" (repeat)            Repeats the last successful non-repeat command.\n"),
                outBold("\th"), out(" (help)              Displays this help menu.\n"),
                outBold("\th*"), out(" (help with stats)   Displays password-tree statistics before this help menu.\n"),
                outBold("\tq"), out(" (quit)              Exits the Passbird application.\n"),
                out("\n"),
                outBold("\tn"), out(" (Nests)             Displays available Nests and related commands.\n"),
                outBold("\tf?"), out(" (Favorites)         Displays Favorites-related usage information.\n"),
                outBold("\tm?"), out(" (Memory)           Displays Memory-related usage information.\n"),
                outBold("\tp?"), out(" (Proteins)         Displays Protein-related usage information.\n"),
                outBold("\ty?"), out(" (Yolks)            Displays Yolk-related usage information.\n"),
                outBold("\ts?"), out(" (Password configs) Displays available password configurations and related help.\n"),
            )
        }
    }

    private fun CanPrintInfo.statsOutputs(): List<Output> {
        val currentNestStats = passwordService.viewNestStats()
        val allNestStats = allNestStats()
        val activeNests = activeNestCount()
        return buildList {
            add(outBold("Stats\n\n"))
            add(outSpecial("Current Nest\n"))
            addAll(statOutputs(currentNestStats))
            add(out("\n"))
            add(outSpecial("Across All Nests\n"))
            addAll(statOutputs(allNestStats, activeNests))
        }
    }

    private fun allNestStats() = nestService.all(includeDefault = true)
        .filter { it.isPresent }
        .map { passwordService.viewNestStats(it.get().slot) }
        .reduce(NestStats(0, 0, 0, 0, 0), ::sum)

    private fun activeNestCount() = nestService.all().filter { it.isPresent }.count().toInt()

    private fun CanPrintInfo.statOutputs(stats: NestStats, activeNests: Int? = null): List<Output> = buildList {
        addAll(statLine("Eggs", stats.eggs))
        activeNests?.let { addAll(statLine("Active Nests", it)) }
        addAll(statLine("Eggs with Yolks", stats.eggsWithYolks))
        addAll(statLine("Eggs with Proteins", stats.eggsWithProteins))
        addAll(statLine("Occupied Protein Slots", stats.occupiedProteinSlots))
        addAll(statLine("Assigned Favorites", stats.assignedFavorites))
    }

    private fun CanPrintInfo.statLine(label: String, value: Int): List<Output> = listOf(out("$label: "), outNest("$value\n"))
}

private fun sum(left: NestStats, right: NestStats) = NestStats(
    eggs = left.eggs + right.eggs,
    eggsWithYolks = left.eggsWithYolks + right.eggsWithYolks,
    eggsWithProteins = left.eggsWithProteins + right.eggsWithProteins,
    occupiedProteinSlots = left.occupiedProteinSlots + right.occupiedProteinSlots,
    assignedFavorites = left.assignedFavorites + right.assignedFavorites,
)
