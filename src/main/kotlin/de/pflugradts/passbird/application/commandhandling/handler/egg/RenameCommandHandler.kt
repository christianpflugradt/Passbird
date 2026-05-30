package de.pflugradts.passbird.application.commandhandling.handler.egg
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.RenameCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
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
) : TypedCommandHandler<RenameCommand>(RenameCommand::class.java) {
    override fun handleCommand(command: RenameCommand) {
        if (passwordService.eggExists(command.argument, CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            val secureInput = userInterfaceAdapterPort.receive(outputOf(shellOf("Enter new EggId or nothing to abort: ")))
            if (secureInput.isEmpty) {
                commandExecutionTracker.markAborted()
                userInterfaceAdapterPort.send(outputOf(shellOf("Empty input - Operation aborted."), OPERATION_ABORTED))
            } else {
                try {
                    if (passwordService.renameEgg(command.argument, secureInput.shell).failure) {
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
        command.invalidateInput()
    }
}
