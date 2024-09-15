package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.GetCommand
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService

class GetCommandHandler @Inject constructor(
    private val passwordService: PasswordService,
    private val clipboardAdapterPort: ClipboardAdapterPort,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleGetCommand(getCommand: GetCommand) {
        // below line results in an arch unit violation due to https://github.com/TNG/ArchUnit/issues/981
        // passwordService.viewPassword(getCommand.argument).ifPresent {
        passwordService.viewPassword(getCommand.argument).orNull()?.also {
            clipboardAdapterPort.post(outputOf(it))
            userInterfaceAdapterPort.send(outputOf(shellOf("Password copied to clipboard.")))
        }
        getCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
