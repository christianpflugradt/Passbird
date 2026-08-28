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
        addAll(primaryCommandOutputs())
        add(out("\n"))
        addAll(secondaryCommandOutputs())
        add(out("\n"))
        addAll(infoCommandOutputs())
    }

    private fun CanPrintInfo.helpIntroOutputs() = listOf(
        outBold("\nUsage: [command][parameter]\n"),
        out("A command takes at most one parameter which is usually an EggId.\n\n"),
        outBold("Commands:\n"),
    )

    private fun primaryCommands() = listOf(
        Triple("g[EggId]", "(get)", "Copies the password for the specified Egg to the clipboard."),
        Triple("s[EggId]", "(set)", "Sets a random password for the specified Egg, overwriting any existing one."),
        Triple("s*[EggId]", "(set once)", "Sets a random password for the specified EggId using a one-time configuration."),
        Triple("c[EggId]", "(custom set)", "Prompts the user to input a custom password for the specified Egg."),
        Triple("v[EggId]", "(view)", "Displays the password for the specified Egg in the console."),
        Triple("r[EggId]", "(rename)", "Renames the specified Egg by prompting the user for a new EggId."),
        Triple("d[EggId]", "(discard)", "Moves the specified Egg to trash."),
        Triple("d![EggId]", "(force)", "Permanently deletes the specified Egg."),
        Triple("d", "(trash)", "Displays trashed Eggs and allows restoring them."),
    )

    private fun secondaryCommands() = listOf(
        Triple("e", "(export)", "Exports the Password Tree to a human-readable JSON file."),
        Triple("e*", "(selective export)", "Exports selected Nests or all Nests except selected Nests."),
        Triple("i", "(import)", "Imports passwords from a JSON file into the Password Tree."),
        Triple("i*", "(selective import)", "Imports one Nest from a JSON file into a selected Nest Slot."),
        Triple("k", "(keystore)", "Changes the master password of the keystore."),
        Triple("l", "(list)", "Lists all EggIds in the current Nest."),
        Triple("l[filter]", null, "Lists EggIds in the current Nest whose name contains filter."),
        Triple("l*", null, "Lists all EggIds across all Nests, grouped by Nest."),
        Triple("l*[filter]", null, "Lists EggIds across all Nests whose name contains filter, grouped by Nest."),
        Triple(".", "(repeat)", "Repeats the last successful non-repeat command."),
        Triple("h", "(help)", "Displays this help menu."),
        Triple("h*", "(help with stats)", "Displays password-tree statistics before this help menu."),
        Triple("q", "(quit)", "Exits the Passbird application."),
    )

    private fun infoCommands() = listOf(
        Triple("n", "(Nests)", "Displays available Nests and related commands."),
        Triple("f?", "(Favorites)", "Displays Favorites-related usage information."),
        Triple("m?", "(Memory)", "Displays Memory-related usage information."),
        Triple("p?", "(Proteins)", "Displays Protein-related usage information."),
        Triple("y?", "(Yolks)", "Displays Yolk-related usage information."),
        Triple("s?", "(Password configs)", "Displays available password configurations and related help."),
    )

    private fun CanPrintInfo.primaryCommandOutputs() = commandSectionOutputs(primaryCommands())

    private fun CanPrintInfo.secondaryCommandOutputs() = commandSectionOutputs(secondaryCommands(), actionColumnWidth = 20)

    private fun CanPrintInfo.infoCommandOutputs() = commandSectionOutputs(infoCommands(), actionColumnWidth = 20)

    private fun CanPrintInfo.commandSectionOutputs(
        commands: List<Triple<String, String?, String>>,
        actionColumnWidth: Int = 14,
    ): List<Output> = buildList {
        commands.forEach { (command, action, description) ->
            addAll(commandInfoOutputs(command, action, description, actionColumnWidth = actionColumnWidth))
        }
    }

    private fun CanPrintInfo.statsOutputs(): List<Output> {
        val currentNestStats = passwordService.viewNestStats()
        val allNestStats = allNestStats()
        val activeNests = activeNestCount()
        return buildList {
            add(outBold("Current Nest\n"))
            addAll(statOutputs(currentNestStats))
            add(out("\n"))
            add(outBold("Across All Nests\n"))
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

    private fun CanPrintInfo.statLine(label: String, value: Int): List<Output> = listOf(out(statLabel(label)), out("$value\n"))
}

private fun sum(left: NestStats, right: NestStats) = NestStats(
    eggs = left.eggs + right.eggs,
    eggsWithYolks = left.eggsWithYolks + right.eggsWithYolks,
    eggsWithProteins = left.eggsWithProteins + right.eggsWithProteins,
    occupiedProteinSlots = left.occupiedProteinSlots + right.occupiedProteinSlots,
    assignedFavorites = left.assignedFavorites + right.assignedFavorites,
)
