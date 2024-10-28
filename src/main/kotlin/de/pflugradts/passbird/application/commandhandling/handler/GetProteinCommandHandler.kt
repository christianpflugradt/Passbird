package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.GetProteinCommand
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting
import de.pflugradts.passbird.domain.service.password.PasswordService

class GetProteinCommandHandler @Inject constructor(
    private val passwordService: PasswordService,
    private val clipboardAdapterPort: ClipboardAdapterPort,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleGetProteinCommand(getProteinCommand: GetProteinCommand) {
        passwordService.viewProteinStructure(getProteinCommand.argument, getProteinCommand.slot).orNull()?.also {
            if (it.isNotEmpty) {
                clipboardAdapterPort.post(outputOf(it))
                userInterfaceAdapterPort.send(outputOf(shellOf("Protein copied to clipboard.")))
            } else {
                val msg = "Specified Protein Structure is empty - Operation aborted."
                userInterfaceAdapterPort.send(outputOf(shellOf(msg), OutputFormatting.OPERATION_ABORTED))
            }
        }
        getProteinCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
