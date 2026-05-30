package de.pflugradts.passbird.application.commandhandling.handler.egg
import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.RenameCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.egg.EggIdException
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT
class RenameCommandHandler constructor(
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : CommandHandler {
    @Subscribe
    private fun handleRenameCommand(renameCommand: RenameCommand) {
        if (passwordService.eggExists(renameCommand.argument, CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            val secureInput = userInterfaceAdapterPort.receive(outputOf(shellOf("Enter new EggId or nothing to abort: ")))
            if (secureInput.isEmpty) {
                commandExecutionTracker.markAborted()
                userInterfaceAdapterPort.send(outputOf(shellOf("Empty input - Operation aborted."), OPERATION_ABORTED))
            } else {
                try {
                    if (passwordService.renameEgg(renameCommand.argument, secureInput.shell).failure) {
                        commandExecutionTracker.markFailure()
                    }
                } catch (ex: EggIdException) {
                    commandExecutionTracker.markAborted()
                    userInterfaceAdapterPort.send(outputOf(shellOf("${ex.message} - Operation aborted."), OPERATION_ABORTED))
                }
            }
            secureInput.invalidate()
        } else {
            commandExecutionTracker.markFailure()
        }
        userInterfaceAdapterPort.sendLineBreak()
        renameCommand.invalidateInput()
    }
}
