package de.pflugradts.passbird.application.commandhandling.handler.nest

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.DiscardNestCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.slotAt
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.PasswordService
import jakarta.inject.Inject

class DiscardNestCommandHandler @Inject constructor(
    private val nestService: NestService,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : CommandHandler {

    @Subscribe
    private fun handleDiscardNestCommand(discardNestCommand: DiscardNestCommand) {
        when {
            discardNestCommand.slot == DEFAULT -> {
                sendAbortMessage("Default Nest cannot be discarded - Operation aborted.")
                return
            }

            nestService.atNestSlot(discardNestCommand.slot).isEmpty -> {
                sendAbortMessage("Specified Nest does not exist - Operation aborted.")
                return
            }
        }
        discardExistingNest(discardNestCommand)
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun discardExistingNest(discardNestCommand: DiscardNestCommand) {
        val currentNest = nestService.currentNest()
        try {
            nestService.moveToNestAt(discardNestCommand.slot)
            val eggIds = passwordService.findAllEggIds().toList()
            if (eggIds.isEmpty()) {
                if (nestService.discardNestAt(discardNestCommand.slot).failure) {
                    commandExecutionTracker.markFailure()
                }
                return
            }
            discardNestWithEggs(discardNestCommand.slot, eggIds)
        } finally {
            nestService.moveToNestAt(if (discardNestCommand.slot == currentNest.slot) DEFAULT else currentNest.slot)
        }
    }

    private fun discardNestWithEggs(discardNestSlot: Slot, eggIds: List<Shell>) {
        val targetNestSlot = receiveTargetNestSlot(eggIds) ?: return
        val overlaps = overlappingEggIds(discardNestSlot, targetNestSlot, eggIds)
        if (overlaps.isNotEmpty()) {
            commandExecutionTracker.markAborted()
            val overlapsMessage = "The following EggIds exist in both Nests. " +
                "Please move them manually before discarding the Nest: ${System.lineSeparator()}- " + joinToString(overlaps)
            userInterfaceAdapterPort.send(outputOf(shellOf(overlapsMessage)))
            userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted.")))
            return
        }
        if (eggIds.any { eggId -> passwordService.moveEgg(eggId, targetNestSlot).failure }) {
            commandExecutionTracker.markFailure()
            return
        }
        if (nestService.discardNestAt(discardNestSlot).failure) {
            commandExecutionTracker.markFailure()
        }
    }

    private fun receiveTargetNestSlot(eggIds: List<Shell>): Slot? {
        val prompt = "Nest '${nestService.currentNest().viewNestId().asString()}' contains ${eggIds.size} Eggs. " +
            "Specify a Nest Slot 0-9 to move them to or anything else to abort: "
        val input = userInterfaceAdapterPort.receive(outputOf(shellOf(prompt)))
        val nestSlot = input.shell.asString()
        if (nestSlot.length != 1 || !nestSlot[0].isDigit()) {
            commandExecutionTracker.markAborted()
            userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
            return null
        }
        val targetNestOption = nestService.atNestSlot(slotAt(nestSlot))
        if (targetNestOption.isEmpty) {
            commandExecutionTracker.markAborted()
            userInterfaceAdapterPort.send(outputOf(shellOf("Nest Slot $nestSlot is empty - Operation aborted."), OPERATION_ABORTED))
            return null
        }
        return targetNestOption.get().slot
    }

    private fun overlappingEggIds(discardNestSlot: Slot, targetNestSlot: Slot, eggIds: List<Shell>): List<Shell> {
        nestService.moveToNestAt(targetNestSlot)
        return try {
            val otherEggIds = passwordService.findAllEggIds().toList()
            eggIds.filter(otherEggIds::contains)
        } finally {
            nestService.moveToNestAt(discardNestSlot)
        }
    }

    private fun joinToString(shells: List<Shell>) = shells.joinToString(separator = "${System.lineSeparator()}- ") { id -> id.asString() }

    private fun sendAbortMessage(message: String) {
        commandExecutionTracker.markAborted()
        userInterfaceAdapterPort.send(outputOf(shellOf(message), OPERATION_ABORTED))
    }
}
