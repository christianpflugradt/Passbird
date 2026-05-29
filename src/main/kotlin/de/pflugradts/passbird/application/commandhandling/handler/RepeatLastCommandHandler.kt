package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.InputHandler
import de.pflugradts.passbird.application.commandhandling.RememberedCommandMemory
import de.pflugradts.passbird.application.commandhandling.command.RepeatLastCommand
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import jakarta.inject.Inject
import jakarta.inject.Provider

class RepeatLastCommandHandler @Inject constructor(
    private val inputHandlerProvider: Provider<InputHandler>,
    private val rememberedCommandMemory: RememberedCommandMemory,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : CommandHandler {
    @Subscribe
    private fun handleRepeatLastCommand(@Suppress("UNUSED_PARAMETER") repeatLastCommand: RepeatLastCommand) {
        val rememberedCommand = rememberedCommandMemory.view()
        if (rememberedCommand == null) {
            commandExecutionTracker.markFailure()
            userInterfaceAdapterPort.send(
                outputOf(shellOf("No previous command is available to repeat."), OPERATION_ABORTED),
            )
            userInterfaceAdapterPort.sendLineBreak()
            return
        }
        inputHandlerProvider.get().handleInput(inputOf(rememberedCommand))
        commandExecutionTracker.mark(commandExecutionTracker.lastCompletedOutcome())
    }
}
