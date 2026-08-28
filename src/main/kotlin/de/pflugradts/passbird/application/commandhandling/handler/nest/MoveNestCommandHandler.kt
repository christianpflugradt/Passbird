package de.pflugradts.passbird.application.commandhandling.handler.nest

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.MoveNestCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.FIRST_SLOT
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.LAST_SLOT
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.slotAt
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.nest.NestService

class MoveNestCommandHandler constructor(
    private val nestService: NestService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<MoveNestCommand>(MoveNestCommand::class.java) {
    override fun handleCommand(command: MoveNestCommand) {
        when {
            command.slot == DEFAULT -> {
                sendAbortMessage("Default Nest cannot be moved - Operation aborted.")
                return
            }

            nestService.atNestSlot(command.slot).isEmpty -> {
                sendAbortMessage("Specified Nest does not exist - Operation aborted.")
                return
            }
        }
        val freeSlots = freeCustomNestSlots()
        val targetSlot = receiveTargetSlot(freeSlots) ?: return
        if (nestService.moveNest(command.slot, targetSlot).failure) {
            commandExecutionTracker.markFailure()
        }
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun receiveTargetSlot(freeSlots: List<Slot>): Slot? {
        userInterfaceAdapterPort.send(
            outputOf(
                shellOf(
                    "Available free Nest Slots: ${freeSlots.joinToString(", ") {
                        it.index().toString()
                    }}",
                ),
            ),
        )
        val input = userInterfaceAdapterPort.receive(outputOf(shellOf("\nEnter free Nest Slot you want to move Nest to: ")))
        val targetSlot = input.shell.asString().toTargetSlot()
        input.invalidate()
        if (targetSlot == null) {
            commandExecutionTracker.markAborted()
            userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
            userInterfaceAdapterPort.sendLineBreak()
            return null
        }
        if (targetSlot !in freeSlots) {
            sendAbortMessage("Specified Nest Slot is not free - Operation aborted.")
            userInterfaceAdapterPort.sendLineBreak()
            return null
        }
        return targetSlot
    }

    private fun freeCustomNestSlots() = (FIRST_SLOT..LAST_SLOT)
        .map(::slotAt)
        .filter { nestService.atNestSlot(it).isEmpty }

    private fun String.toTargetSlot(): Slot? = takeIf { it.length == 1 && it[0].isDigit() }?.let(::slotAt)

    private fun sendAbortMessage(message: String) {
        commandExecutionTracker.markAborted()
        userInterfaceAdapterPort.send(outputOf(shellOf(message), OPERATION_ABORTED))
    }
}
