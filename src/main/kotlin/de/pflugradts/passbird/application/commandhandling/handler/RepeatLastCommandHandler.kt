package de.pflugradts.passbird.application.commandhandling.handler

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.InputHandler
import de.pflugradts.passbird.application.commandhandling.RememberedCommandMemory
import de.pflugradts.passbird.application.commandhandling.command.RepeatLastCommand
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED

class RepeatLastCommandHandler(
    private val inputHandlerProvider: () -> InputHandler,
    private val rememberedCommandMemory: RememberedCommandMemory,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<RepeatLastCommand>(RepeatLastCommand::class.java) {
    override fun handleCommand(@Suppress("UNUSED_PARAMETER") command: RepeatLastCommand) {
        val rememberedCommand = rememberedCommandMemory.view()
        if (rememberedCommand == null) {
            commandExecutionTracker.markFailure()
            userInterfaceAdapterPort.send(
                outputOf(shellOf("No previous command is available to repeat."), OPERATION_ABORTED),
            )
            userInterfaceAdapterPort.sendLineBreak()
            return
        }
        inputHandlerProvider().handleInput(inputOf(rememberedCommand))
        commandExecutionTracker.mark(commandExecutionTracker.lastCompletedOutcome())
    }
}
