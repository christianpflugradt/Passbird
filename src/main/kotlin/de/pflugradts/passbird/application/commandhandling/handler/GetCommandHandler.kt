package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.GetCommand
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService
import kotlin.jvm.optionals.getOrNull

class GetCommandHandler @Inject constructor(
    @Inject private val passwordService: PasswordService,
    @Inject private val clipboardAdapterPort: ClipboardAdapterPort,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleGetCommand(getCommand: GetCommand) {
        // below line results in an arch unit violation due to https://github.com/TNG/ArchUnit/issues/981
        // passwordService.viewPassword(getCommand.argument).ifPresent {
        passwordService.viewPassword(getCommand.argument).getOrNull()?.also {
            clipboardAdapterPort.post(outputOf(it))
            userInterfaceAdapterPort.send(outputOf(bytesOf("Password copied to clipboard.")))
        }
        getCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
