package de.pflugradts.passbird.application.commandhandling.handler.nest
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.DiscardNestCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.slotAt
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.PasswordService
class DiscardNestCommandHandler constructor(
    private val nestService: NestService,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<DiscardNestCommand>(DiscardNestCommand::class.java) {
    override fun handleCommand(command: DiscardNestCommand) {
        when {
            command.slot == DEFAULT -> {
                sendAbortMessage("Default Nest cannot be discarded - Operation aborted.")
                return
            }

            nestService.atNestSlot(command.slot).isEmpty -> {
                sendAbortMessage("Specified Nest does not exist - Operation aborted.")
                return
            }
        }
        discardExistingNest(command)
        userInterfaceAdapterPort.sendLineBreak()
    }
    private fun discardExistingNest(command: DiscardNestCommand) {
        val currentNest = nestService.currentNest()
        try {
            nestService.moveToNestAt(command.slot)
            val eggIds = passwordService.findAllEggIds().toList()
            try {
                if (eggIds.isEmpty()) {
                    if (nestService.discardNestAt(command.slot).failure) {
                        commandExecutionTracker.markFailure()
                    }
                    return
                }
                discardNestWithEggs(command.slot, eggIds)
            } finally {
                eggIds.scrambleShells()
            }
        } finally {
            nestService.moveToNestAt(if (command.slot == currentNest.slot) DEFAULT else currentNest.slot)
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
        val otherEggIds = mutableListOf<Shell>()
        return try {
            otherEggIds += passwordService.findAllEggIds().toList()
            eggIds.filter(otherEggIds::contains)
        } finally {
            otherEggIds.scrambleShells()
            nestService.moveToNestAt(discardNestSlot)
        }
    }
    private fun joinToString(shells: List<Shell>) = shells.joinToString(separator = "${System.lineSeparator()}- ") { id -> id.asString() }
    private fun sendAbortMessage(message: String) {
        commandExecutionTracker.markAborted()
        userInterfaceAdapterPort.send(outputOf(shellOf(message), OPERATION_ABORTED))
    }
}
private fun Iterable<Shell>.scrambleShells() = forEach(Shell::scramble)
