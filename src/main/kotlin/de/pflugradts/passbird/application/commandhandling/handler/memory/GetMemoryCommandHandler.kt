package de.pflugradts.passbird.application.commandhandling.handler.memory
import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.GetMemoryCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.application.useScrambled
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService
class GetMemoryCommandHandler constructor(
    private val passwordService: PasswordService,
    private val clipboardAdapterPort: ClipboardAdapterPort,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<GetMemoryCommand>(GetMemoryCommand::class.java) {
    override fun handleCommand(command: GetMemoryCommand) {
        passwordService.viewMemoryEntry(command.slot).ifPresentOrElse(
            block = { memory ->
                memory.useScrambled {
                    val clipboardResult = clipboardAdapterPort.post(outputOf(it))
                    if (clipboardResult.failure) {
                        commandExecutionTracker.markFailure()
                    }
                    clipboardResult.onSuccess {
                        userInterfaceAdapterPort.send(outputOf(shellOf("EggId copied to clipboard.")))
                    }
                }
            },
            other = {
                commandExecutionTracker.markFailure()
                userInterfaceAdapterPort.send(outputOf(shellOf("Memory entry at slot ${command.slot.index()} does not exist.")))
            },
        )
        userInterfaceAdapterPort.sendLineBreak()
    }
}
