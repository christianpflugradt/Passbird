package de.pflugradts.passbird.application.commandhandling.handler.nest

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.DiscardNestCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.slotAt
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.PasswordService

class DiscardNestCommandHandler @Inject constructor(
    @Inject private val nestService: NestService,
    @Inject private val passwordService: PasswordService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {

    @Subscribe
    private fun handleDiscardNestCommand(discardNestCommand: DiscardNestCommand) {
        if (discardNestCommand.slot == DEFAULT) {
            userInterfaceAdapterPort.send(outputOf(shellOf("Default Nest cannot be discarded - Operation aborted."), OPERATION_ABORTED))
            return
        } else if (nestService.atNestSlot(discardNestCommand.slot).isEmpty) {
            userInterfaceAdapterPort.send(outputOf(shellOf("Specified Nest does not exist - Operation aborted."), OPERATION_ABORTED))
            return
        } else {
            val currentNest = nestService.currentNest()
            nestService.moveToNestAt(discardNestCommand.slot)
            val eggIds = passwordService.findAllEggIds().toList()
            if (eggIds.isEmpty()) {
                nestService.discardNestAt(discardNestCommand.slot)
            } else {
                val prompt = "Nest '${nestService.currentNest().viewNestId().asString()}' contains ${eggIds.size} Eggs. " +
                    "Specify a Nest Slot 0-9 to move them to or anything else to abort: "
                val input = userInterfaceAdapterPort.receive(outputOf(shellOf(prompt)))
                val nestSlot = input.shell.asString()
                if (nestSlot.length == 1 && nestSlot[0].isDigit()) {
                    val targetNestOption = nestService.atNestSlot(slotAt(nestSlot))
                    if (targetNestOption.isPresent) {
                        nestService.moveToNestAt(targetNestOption.get().slot)
                        val otherEggIds = passwordService.findAllEggIds().toList()
                        val overlaps = eggIds.filter { eggId -> otherEggIds.contains(eggId) }
                        if (overlaps.isEmpty()) {
                            nestService.moveToNestAt(discardNestCommand.slot)
                            eggIds.forEach { eggId -> passwordService.moveEgg(eggId, targetNestOption.get().slot) }
                            nestService.discardNestAt(discardNestCommand.slot)
                        } else {
                            val overlapsMessage = "The following EggIds exist in both Nests. " +
                                "Please move them manually before discarding the Nest: ${System.lineSeparator()}- " + joinToString(overlaps)
                            userInterfaceAdapterPort.send(outputOf(shellOf(overlapsMessage)))
                            userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted.")))
                        }
                    } else {
                        userInterfaceAdapterPort.send(
                            outputOf(shellOf("Nest Slot $nestSlot is empty - Operation aborted."), OPERATION_ABORTED),
                        )
                    }
                } else {
                    userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
                }
            }
            nestService.moveToNestAt(if (discardNestCommand.slot == currentNest.slot) DEFAULT else currentNest.slot)
        }
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun joinToString(shells: List<Shell>) = shells.joinToString(separator = "${System.lineSeparator()}- ") { id -> id.asString() }
}
