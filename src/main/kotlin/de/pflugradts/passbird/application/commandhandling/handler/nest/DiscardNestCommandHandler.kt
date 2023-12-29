package de.pflugradts.passbird.application.commandhandling.handler.nest

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.DiscardNestCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.nest.NestSlot.Companion.nestSlotAt
import de.pflugradts.passbird.domain.model.nest.NestSlot.DEFAULT
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.PasswordService

class DiscardNestCommandHandler @Inject constructor(
    @Inject private val nestService: NestService,
    @Inject private val passwordService: PasswordService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {

    @Subscribe
    private fun handleDiscardNestCommand(discardNestCommand: DiscardNestCommand) {
        if (discardNestCommand.nestSlot == DEFAULT) {
            userInterfaceAdapterPort.send(outputOf(shellOf("Default Nest cannot be discarded - Operation aborted.")))
            return
        } else if (nestService.atNestSlot(discardNestCommand.nestSlot).isEmpty) {
            userInterfaceAdapterPort.send(outputOf(shellOf("Specified Nest does not exist - Operation aborted.")))
            return
        } else {
            val currentNest = nestService.currentNest()
            nestService.moveToNestAt(discardNestCommand.nestSlot)
            val eggIds = passwordService.findAllEggIds().toList()
            if (eggIds.isEmpty()) {
                nestService.discard(discardNestCommand.nestSlot)
            } else {
                val prompt = "Nest '${nestService.currentNest().shell.asString()}' contains ${eggIds.size} Eggs. " +
                    "Specify a Nest Slot 0-9 to move them to or anything else to abort: "
                val input = userInterfaceAdapterPort.receive(outputOf(shellOf(prompt)))
                val nestSlot = input.shell.asString()
                if (nestSlot.length == 1 && nestSlot[0].isDigit()) {
                    val targetNestOptional = nestService.atNestSlot(nestSlotAt(nestSlot))
                    if (targetNestOptional.isPresent) {
                        nestService.moveToNestAt(targetNestOptional.get().nestSlot)
                        val otherEggIds = passwordService.findAllEggIds().toList()
                        val overlaps = eggIds.filter { eggId -> otherEggIds.contains(eggId) }
                        if (overlaps.isEmpty()) {
                            nestService.moveToNestAt(discardNestCommand.nestSlot)
                            eggIds.forEach { eggId -> passwordService.moveEgg(eggId, targetNestOptional.get().nestSlot) }
                            nestService.discard(discardNestCommand.nestSlot)
                        } else {
                            val overlapsMessage = "The following EggIds exist in both Nests. " +
                                "Please move them manually before discarding the Nest: ${System.lineSeparator()}- " +
                                overlaps.joinToString(separator = "${System.lineSeparator()}- ") { id -> id.asString() }
                            userInterfaceAdapterPort.send(outputOf(shellOf(overlapsMessage)))
                            userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted.")))
                        }
                    } else {
                        userInterfaceAdapterPort.send(outputOf(shellOf("Nest Slot $nestSlot is empty - Operation aborted.")))
                    }
                } else {
                    userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted.")))
                }
            }
            nestService.moveToNestAt(if (discardNestCommand.nestSlot == currentNest.nestSlot) DEFAULT else currentNest.nestSlot)
        }
        userInterfaceAdapterPort.sendLineBreak()
    }
}
