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
            userInterfaceAdapterPort.send(*helpOutputs(command).toTypedArray())
        }
    }

    private fun CanPrintInfo.helpOutputs(command: HelpCommand): List<Output> = buildList {
        if (command.showStats) {
            addAll(statsOutputs())
        }
        addAll(helpIntroOutputs())
        addAll(commandSectionOutputs(primaryCommands()))
        add(out("\n"))
        addAll(commandSectionOutputs(secondaryCommands()))
        add(out("\n"))
        addAll(commandSectionOutputs(infoCommands()))
    }

    private fun CanPrintInfo.helpIntroOutputs() = listOf(
        outBold("\nUsage: [command][parameter]\n"),
        out("A command takes at most one parameter which is usually an EggId.\n\n"),
        outBold("Commands:\n"),
    )

    private fun primaryCommands() = listOf(
        "g[EggId]" to " (get)        Copies the password for the specified Egg to the clipboard.\n",
        "s[EggId]" to " (set)        Sets a random password for the specified Egg, overwriting any existing one.\n",
        "s*[EggId]" to " (set once)   Sets a random password for the specified EggId using a one-time configuration.\n",
        "c[EggId]" to " (custom set) Prompts the user to input a custom password for the specified Egg.\n",
        "v[EggId]" to " (view)       Displays the password for the specified Egg in the console.\n",
        "r[EggId]" to " (rename)     Renames the specified Egg by prompting the user for a new EggId.\n",
        "d[EggId]" to " (discard)    Moves the specified Egg to trash.\n",
        "d![EggId]" to " (force)      Permanently deletes the specified Egg.\n",
        "d" to " (trash)      Displays trashed Eggs and allows restoring them.\n",
    )

    private fun secondaryCommands() = listOf(
        "e" to " (export)            Exports the Password Tree to a human-readable JSON file.\n",
        "e*" to " (selective export)  Exports selected Nests or all Nests except selected Nests.\n",
        "i" to " (import)            Imports passwords from a JSON file into the Password Tree.\n",
        "i*" to " (selective import)  Imports one Nest from a JSON file into a selected Nest Slot.\n",
        "k" to " (keystore)          Changes the master password of the keystore.\n",
        "l" to " (list)              Lists all EggIds in the current Nest.\n",
        "l[filter]" to "              Lists EggIds in the current Nest whose name contains filter.\n",
        "l*" to "                    Lists all EggIds across all Nests, grouped by Nest.\n",
        "l*[filter]" to "             Lists EggIds across all Nests whose name contains filter, grouped by Nest.\n",
        "." to " (repeat)            Repeats the last successful non-repeat command.\n",
        "h" to " (help)              Displays this help menu.\n",
        "h*" to " (help with stats)   Displays password-tree statistics before this help menu.\n",
        "q" to " (quit)              Exits the Passbird application.\n",
    )

    private fun infoCommands() = listOf(
        "n" to " (Nests)             Displays available Nests and related commands.\n",
        "f?" to " (Favorites)         Displays Favorites-related usage information.\n",
        "m?" to " (Memory)            Displays Memory-related usage information.\n",
        "p?" to " (Proteins)          Displays Protein-related usage information.\n",
        "y?" to " (Yolks)             Displays Yolk-related usage information.\n",
        "s?" to " (Password configs)  Displays available password configurations and related help.\n",
    )

    private fun CanPrintInfo.commandSectionOutputs(commands: List<Pair<String, String>>): List<Output> = buildList {
        commands.forEach { (command, description) ->
            addAll(commandInfoOutputs(command, description))
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

    private fun CanPrintInfo.statLine(label: String, value: Int): List<Output> = listOf(out(statLabel(label)), outNest("$value\n"))
}

private fun sum(left: NestStats, right: NestStats) = NestStats(
    eggs = left.eggs + right.eggs,
    eggsWithYolks = left.eggsWithYolks + right.eggsWithYolks,
    eggsWithProteins = left.eggsWithProteins + right.eggsWithProteins,
    occupiedProteinSlots = left.occupiedProteinSlots + right.occupiedProteinSlots,
    assignedFavorites = left.assignedFavorites + right.assignedFavorites,
)
