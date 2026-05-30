package de.pflugradts.passbird.application.commandhandling.handler
import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.ListCommand
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.PasswordService
class ListCommandHandler constructor(
    private val nestService: NestService,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleListCommand(listCommand: ListCommand) {
        val output = if (listCommand.showAll) {
            groupByNest(listCommand.argument)
        } else {
            listCurrentNest(listCommand.argument)
        }
        userInterfaceAdapterPort.send(outputOf(output))
        listCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
    private fun listCurrentNest(filter: Shell): Shell {
        val eggIds = passwordService.findAllEggIds().toList()
        return try {
            join(filter(eggIds, filter))
        } finally {
            eggIds.scrambleShells()
        }
    }
    private fun join(eggIdShellList: List<Shell>) = if (eggIdShellList.isEmpty()) {
        shellOf("Nest is empty")
    } else {
        shellOf(eggIdShellList.joinToString(", ") { it.asString() })
    }
    private fun groupByNest(filter: Shell): Shell {
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
                shellOf("Nest is empty")
            } else {
                shellOf(
                    eggIdsByNest.joinToString("\n") { (nest, eggIds) ->
                        "${nest.label()}\n\t${eggIds.joinToString(", ") { eggId -> eggId.asString() }}"
                    },
                )
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
}
private fun Iterable<Shell>.scrambleShells() = forEach(Shell::scramble)
private fun Nest.label() = if (slot == Slot.DEFAULT) {
    "0: ${viewNestId().asString()}"
} else {
    "${slot.index()}: ${viewNestId().asString()}"
}
