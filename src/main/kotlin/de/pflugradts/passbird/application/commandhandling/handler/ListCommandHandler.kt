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
import jakarta.inject.Inject

class ListCommandHandler @Inject constructor(
    private val nestService: NestService,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleListCommand(listCommand: ListCommand) {
        val output = if (listCommand.showAll) {
            groupByNest(listCommand.argument)
        } else {
            join(filter(passwordService.findAllEggIds().toList(), listCommand.argument))
        }
        userInterfaceAdapterPort.send(outputOf(output))
        listCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun join(eggIdShellList: List<Shell>) = if (eggIdShellList.isEmpty()) {
        shellOf("Nest is empty")
    } else {
        shellOf(eggIdShellList.joinToString(", ") { it.asString() })
    }

    private fun groupByNest(filter: Shell): Shell = nestService.all(includeDefault = true)
        .filter { it.isPresent }
        .map { it.get() }
        .map { nest -> nest to filter(passwordService.findAllEggIds(nest.slot).toList(), filter) }
        .filter { it.second.isNotEmpty() }
        .toList()
        .let {
            if (it.isEmpty()) {
                shellOf("Nest is empty")
            } else {
                shellOf(
                    it.joinToString("\n") { (nest, eggIds) ->
                        "${nest.label()}\n\t${eggIds.joinToString(", ") { eggId -> eggId.asString() }}"
                    },
                )
            }
        }

    private fun filter(eggIds: List<Shell>, filter: Shell) = if (filter.isEmpty) {
        eggIds
    } else {
        val searchTerm = filter.asString()
        eggIds.filter { it.asString().contains(searchTerm, ignoreCase = true) }
    }
}

private fun Nest.label() = if (slot == Slot.DEFAULT) {
    "0: ${viewNestId().asString()}"
} else {
    "${slot.index()}: ${viewNestId().asString()}"
}
