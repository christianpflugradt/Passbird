package de.pflugradts.passbird.application.commandhandling.handler.memory

import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.InputHandler
import de.pflugradts.passbird.application.commandhandling.command.UseMemoryCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
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
) : TypedCommandHandler<UseMemoryCommand>(UseMemoryCommand::class.java) {
    override fun handleCommand(command: UseMemoryCommand) {
        passwordService.viewMemoryEntry(command.slot).ifPresentOrElse(
            block = { memory ->
                memory.useScrambled {
                    inputHandler().handleInput(inputOf(command.argument + it))
                    commandExecutionTracker.mark(commandExecutionTracker.lastCompletedOutcome())
                }
            },
            other = {
                commandExecutionTracker.markFailure()
                userInterfaceAdapterPort.send(outputOf(shellOf("Memory entry at slot ${command.slot.index()} does not exist.")))
            },
        )
        command.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
