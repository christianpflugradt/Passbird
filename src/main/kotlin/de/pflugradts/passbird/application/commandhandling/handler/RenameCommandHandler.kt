package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.RenameCommand
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EntryNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT

class RenameCommandHandler @Inject constructor(
    @Inject private val passwordService: PasswordService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleRenameCommand(renameCommand: RenameCommand) {
        if (passwordService.entryExists(renameCommand.argument, CREATE_ENTRY_NOT_EXISTS_EVENT)) {
            val secureInput = userInterfaceAdapterPort.receive(outputOf(bytesOf("Enter new alias or nothing to abort: ")))
            if (secureInput.isEmpty) {
                userInterfaceAdapterPort.send(outputOf(bytesOf("Empty input - Operation aborted.")))
            } else {
                passwordService.renamePasswordEntry(renameCommand.argument, secureInput.bytes)
            }
            secureInput.invalidate()
        }
        userInterfaceAdapterPort.sendLineBreak()
        renameCommand.invalidateInput()
    }
}
