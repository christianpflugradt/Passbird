package de.pflugradts.passbird.application.commandhandling.handler.nest

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.capabilities.CanListAvailableNests
import de.pflugradts.passbird.application.commandhandling.command.MoveToNestCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.slot.Slot.Companion.slotAt
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.HIGHLIGHT
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction
import jakarta.inject.Inject

class MoveToNestCommandHandler @Inject constructor(
    private val canListAvailableNests: CanListAvailableNests,
    private val nestService: NestService,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : CommandHandler {
    @Subscribe
    private fun handleMoveToNestCommand(moveToNestCommand: MoveToNestCommand) {
        if (passwordService.eggExists(moveToNestCommand.argument, EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            userInterfaceAdapterPort.send(outputOf(shellOf("\nAvailable Nests: \n"), HIGHLIGHT))
            userInterfaceAdapterPort.send(outputOf(shellOf(canListAvailableNests.getAvailableNests(includeCurrent = false))))
            val input = userInterfaceAdapterPort.receive(outputOf(shellOf("\nEnter Nest you want to move Egg to: ")))
            val nestSlot = input.shell.asString().toTargetNestSlot()
            if (nestSlot == null) {
                commandExecutionTracker.markAborted()
                userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
            } else if (nestSlot === nestService.currentNest().slot) {
                commandExecutionTracker.markAborted()
                userInterfaceAdapterPort.send(
                    outputOf(shellOf("Egg is already in the specified Nest - Operation aborted."), OPERATION_ABORTED),
                )
            } else if (nestService.atNestSlot(nestSlot).isEmpty) {
                commandExecutionTracker.markAborted()
                userInterfaceAdapterPort.send(outputOf(shellOf("Specified Nest does not exist - Operation aborted."), OPERATION_ABORTED))
            } else if (passwordService.eggExists(moveToNestCommand.argument, nestSlot)) {
                commandExecutionTracker.markAborted()
                userInterfaceAdapterPort.send(
                    outputOf(shellOf("Egg with same EggId already exists in target Nest - Operation aborted."), OPERATION_ABORTED),
                )
            } else {
                if (passwordService.moveEgg(moveToNestCommand.argument, nestSlot).failure) {
                    commandExecutionTracker.markFailure()
                }
            }
            input.invalidate()
        } else {
            commandExecutionTracker.markFailure()
        }
        moveToNestCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun String.toTargetNestSlot(): Slot? = takeIf { it.length == 1 && it[0].isDigit() }?.let(::slotAt)
}
