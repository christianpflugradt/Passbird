package de.pflugradts.passbird.application.commandhandling.handler.memory

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.InputHandler
import de.pflugradts.passbird.application.commandhandling.command.UseMemoryCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.application.useScrambled
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService

class UseMemoryCommandHandler(
    private val inputHandler: () -> InputHandler,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : CommandHandler {
    @Subscribe
    private fun handleUseMemoryCommand(useMemoryCommand: UseMemoryCommand) {
        passwordService.viewMemoryEntry(useMemoryCommand.slot).ifPresentOrElse(
            block = { memory ->
                memory.useScrambled {
                    inputHandler().handleInput(inputOf(useMemoryCommand.argument + it))
                    commandExecutionTracker.mark(commandExecutionTracker.lastCompletedOutcome())
                }
            },
            other = {
                commandExecutionTracker.markFailure()
                userInterfaceAdapterPort.send(outputOf(shellOf("Memory entry at slot ${useMemoryCommand.slot.index()} does not exist.")))
            },
        )
        useMemoryCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
