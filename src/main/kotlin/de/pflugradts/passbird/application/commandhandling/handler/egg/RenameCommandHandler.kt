package de.pflugradts.passbird.application.commandhandling.handler.egg

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.RenameCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.egg.EggIdException
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT

class RenameCommandHandler @Inject constructor(
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleRenameCommand(renameCommand: RenameCommand) {
        if (passwordService.eggExists(renameCommand.argument, CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            val secureInput = userInterfaceAdapterPort.receive(outputOf(shellOf("Enter new EggId or nothing to abort: ")))
            if (secureInput.isEmpty) {
                userInterfaceAdapterPort.send(outputOf(shellOf("Empty input - Operation aborted."), OPERATION_ABORTED))
            } else {
                try {
                    passwordService.renameEgg(renameCommand.argument, secureInput.shell)
                } catch (ex: EggIdException) {
                    userInterfaceAdapterPort.send(outputOf(shellOf("${ex.message} - Operation aborted."), OPERATION_ABORTED))
                }
            }
            secureInput.invalidate()
        }
        userInterfaceAdapterPort.sendLineBreak()
        renameCommand.invalidateInput()
    }
}
