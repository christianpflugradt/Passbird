package de.pflugradts.passbird.application.commandhandling.handler.memory

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.InputHandler
import de.pflugradts.passbird.application.commandhandling.command.UseMemoryCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService
import jakarta.inject.Inject

class UseMemoryCommandHandler @Inject constructor(
    private val inputHandler: InputHandler,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleUseMemoryCommand(useMemoryCommand: UseMemoryCommand) {
        passwordService.viewMemoryEntry(useMemoryCommand.slot).ifPresentOrElse(
            block = {
                inputHandler.handleInput(inputOf(useMemoryCommand.argument + it))
                CommandExecutionTracker.mark(CommandExecutionTracker.lastCompletedOutcome())
            },
            other = {
                CommandExecutionTracker.markFailure()
                userInterfaceAdapterPort.send(outputOf(shellOf("Memory entry at slot ${useMemoryCommand.slot.index()} does not exist.")))
            },
        )
        useMemoryCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
