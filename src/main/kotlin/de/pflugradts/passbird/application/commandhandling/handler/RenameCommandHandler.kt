package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.RenameCommand
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT

class RenameCommandHandler @Inject constructor(
    @Inject private val passwordService: PasswordService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleRenameCommand(renameCommand: RenameCommand) {
        if (passwordService.eggExists(renameCommand.argument, CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            val secureInput = userInterfaceAdapterPort.receive(outputOf(shellOf("Enter new EggId or nothing to abort: ")))
            if (secureInput.isEmpty) {
                userInterfaceAdapterPort.send(outputOf(shellOf("Empty input - Operation aborted.")))
            } else {
                passwordService.renameEgg(renameCommand.argument, secureInput.shell)
            }
            secureInput.invalidate()
        }
        userInterfaceAdapterPort.sendLineBreak()
        renameCommand.invalidateInput()
    }
}
