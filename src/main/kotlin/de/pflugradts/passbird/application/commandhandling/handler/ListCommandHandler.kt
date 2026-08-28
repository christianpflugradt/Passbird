package de.pflugradts.passbird.application.commandhandling.handler
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.ListCommand
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.DEFAULT
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.HIGHLIGHT
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.SPECIAL
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.PasswordService

class ListCommandHandler constructor(
    private val nestService: NestService,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : TypedCommandHandler<ListCommand>(ListCommand::class.java) {
    override fun handleCommand(command: ListCommand) {
        val output = if (command.showAll) {
            groupByNest(command.argument)
        } else {
            listCurrentNest(command.argument)
        }
        userInterfaceAdapterPort.send(*output.toTypedArray())
        command.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun listCurrentNest(filter: Shell): List<de.pflugradts.passbird.domain.model.transfer.Output> {
        val eggIds = passwordService.findAllEggIds().toList()
        return try {
            renderEggIds(filter(eggIds, filter), filter)
        } finally {
            eggIds.scrambleShells()
        }
    }

    private fun groupByNest(filter: Shell): List<de.pflugradts.passbird.domain.model.transfer.Output> {
        val eggIdShells = mutableListOf<Shell>()
        return try {
            val eggIdsByNest = nestService.all(includeDefault = true)
                .filter { it.isPresent }
                .map { it.get() }
                .map { nest ->
                    val eggIds = passwordService.findAllEggIds(nest.slot).toList()
                    eggIdShells += eggIds
                    nest to filter(eggIds, filter)
                }
                .filter { it.second.isNotEmpty() }
                .toList()
            if (eggIdsByNest.isEmpty()) {
                listOf(outputOf(shellOf("Nest is empty")))
            } else {
                buildList {
                    eggIdsByNest.forEachIndexed { index, (nest, eggIds) ->
                        if (index > 0) add(outputOf(shellOf("\n")))
                        add(outputOf(shellOf(nest.label()), HIGHLIGHT))
                        add(outputOf(shellOf("\n\t")))
                        addAll(renderEggIds(eggIds, filter))
                    }
                }
            }
        } finally {
            eggIdShells.scrambleShells()
        }
    }

    private fun filter(eggIds: List<Shell>, filter: Shell) = if (filter.isEmpty) {
        eggIds
    } else {
        val searchTerm = filter.asString()
        eggIds.filter { it.asString().contains(searchTerm, ignoreCase = true) }
    }

    private fun renderEggIds(eggIds: List<Shell>, filter: Shell) = if (eggIds.isEmpty()) {
        listOf(outputOf(shellOf("Nest is empty")))
    } else {
        buildList {
            eggIds.forEachIndexed { index, eggId ->
                if (index > 0) add(outputOf(shellOf(", ")))
                addAll(renderEggId(eggId, filter))
            }
        }
    }

    private fun renderEggId(eggId: Shell, filter: Shell) = if (filter.isEmpty) {
        listOf(outputOf(shellOf(eggId.asString()), DEFAULT))
    } else {
        highlightMatches(eggId.asString(), filter.asString())
    }

    private fun highlightMatches(eggId: String, searchTerm: String) = buildList {
        var nextIndex = 0
        while (nextIndex < eggId.length) {
            val matchIndex = eggId.indexOf(searchTerm, startIndex = nextIndex, ignoreCase = true)
            if (matchIndex < 0) {
                addDefaultFragment(eggId.substring(nextIndex))
                break
            }
            addDefaultFragment(eggId.substring(nextIndex, matchIndex))
            add(outputOf(shellOf(eggId.substring(matchIndex, matchIndex + searchTerm.length)), SPECIAL))
            nextIndex = matchIndex + searchTerm.length
        }
    }

    private fun MutableList<de.pflugradts.passbird.domain.model.transfer.Output>.addDefaultFragment(text: String) {
        if (text.isNotEmpty()) add(outputOf(shellOf(text), DEFAULT))
    }
}

private fun Iterable<Shell>.scrambleShells() = forEach(Shell::scramble)
private fun Nest.label() = if (slot == Slot.DEFAULT) {
    "0: ${viewNestId().asString()}"
} else {
    "${slot.index()}: ${viewNestId().asString()}"
}
