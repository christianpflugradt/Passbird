package de.pflugradts.passbird.application.commandhandling.handler.memory

import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.GetMemoryCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService
import jakarta.inject.Inject

class GetMemoryCommandHandler @Inject constructor(
    private val passwordService: PasswordService,
    private val clipboardAdapterPort: ClipboardAdapterPort,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleGetMemoryCommand(getMemoryCommand: GetMemoryCommand) {
        passwordService.viewMemoryEntry(getMemoryCommand.slot).ifPresentOrElse(
            block = {
                val clipboardResult = clipboardAdapterPort.post(outputOf(it))
                if (clipboardResult.failure) {
                    CommandExecutionTracker.markFailure()
                }
                clipboardResult.onSuccess {
                    userInterfaceAdapterPort.send(outputOf(shellOf("EggId copied to clipboard.")))
                }
            },
            other = {
                CommandExecutionTracker.markFailure()
                userInterfaceAdapterPort.send(outputOf(shellOf("Memory entry at slot ${getMemoryCommand.slot.index()} does not exist.")))
            },
        )
        userInterfaceAdapterPort.sendLineBreak()
    }
}
