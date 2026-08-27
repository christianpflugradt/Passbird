package de.pflugradts.passbird.application.commandhandling.handler.egg

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.ViewTrashCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.RestoreEggResult
import de.pflugradts.passbird.domain.service.password.TrashEggView

class ViewTrashCommandHandler constructor(
    private val configuration: ReadableConfiguration,
    private val nestService: NestService,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<ViewTrashCommand>(ViewTrashCommand::class.java) {
    override fun handleCommand(command: ViewTrashCommand) {
        while (true) {
            val trashedEggs = passwordService.viewTrash()
            if (trashedEggs.isEmpty()) {
                userInterfaceAdapterPort.send(outputOf(shellOf("Trash is empty")))
                userInterfaceAdapterPort.sendLineBreak()
                trashedEggs.scrambleEggIds()
                return
            }
            val input = userInterfaceAdapterPort.receive(outputOf(shellOf(trashText(trashedEggs))))
            if (input.shell.isEmpty) {
                input.invalidate()
                userInterfaceAdapterPort.sendLineBreak()
                trashedEggs.scrambleEggIds()
                return
            }
            val index = input.shell.asString().toIntOrNull()
            if (index == null || index !in trashedEggs.indices) {
                commandExecutionTracker.markAborted()
                userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
                userInterfaceAdapterPort.sendLineBreak()
                input.invalidate()
                trashedEggs.scrambleEggIds()
                continue
            }
            val selectedEgg = trashedEggs[index]
            if (restoreConfirmed(selectedEgg.eggId)) {
                val restoreResult = passwordService.restoreEgg(selectedEgg.eggId)
                if (restoreResult.failure) {
                    commandExecutionTracker.markFailure()
                } else {
                    when (restoreResult.getOrNull()!!) {
                        RestoreEggResult.RESTORED -> Unit

                        RestoreEggResult.RESTORED_TO_DEFAULT ->
                            userInterfaceAdapterPort.send(
                                outputOf(shellOf("Original Nest no longer exists. Egg will be restored to Default Nest.")),
                            )

                        RestoreEggResult.TARGET_CONFLICT -> {
                            commandExecutionTracker.markAborted()
                            userInterfaceAdapterPort.send(
                                outputOf(
                                    shellOf("Egg with same EggId already exists in target Nest - Operation aborted."),
                                    OPERATION_ABORTED,
                                ),
                            )
                        }

                        RestoreEggResult.NOT_FOUND -> commandExecutionTracker.markFailure()
                    }
                }
            } else {
                commandExecutionTracker.markAborted()
                userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
            }
            input.invalidate()
            trashedEggs.scrambleEggIds()
            userInterfaceAdapterPort.sendLineBreak()
        }
    }

    private fun restoreConfirmed(eggId: Shell) = if (configuration.application.password.promptOnRemoval) {
        userInterfaceAdapterPort.receiveConfirmation(
            outputOf(
                shellOf(
                    """
                    Restoring Egg '${eggId.asString()}'.
                    Input 'c' to confirm or anything else to abort.
                    Your input: 
                    """.trimIndent(),
                ),
            ),
        )
    } else {
        true
    }

    private fun trashText(trashedEggs: List<TrashEggView>) = buildString {
        append("Trash")
        trashedEggs.forEachIndexed { index, egg ->
            appendLine()
            append("[$index]\t${nestName(egg.nestSlot)}/${egg.eggId.asString()}\t${egg.deletionAgeDays}")
        }
        appendLine()
        append("Enter index to restore Egg or just press enter to abort: ")
    }

    private fun nestName(slot: Slot) = nestService.atNestSlot(slot).map { it.viewNestId().asString() }.orElse("Default")

    private fun List<TrashEggView>.scrambleEggIds() = forEach { it.eggId.scramble() }
}
