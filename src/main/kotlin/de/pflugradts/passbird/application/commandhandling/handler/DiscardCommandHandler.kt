package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.DiscardCommand
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.password.PasswordService

class DiscardCommandHandler @Inject constructor(
    private val configuration: ReadableConfiguration,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleDiscardCommand(discardCommand: DiscardCommand) {
        if (passwordService.viewPassword(discardCommand.argument).isPresent) {
            if (commandConfirmed()) {
                passwordService.discardEgg(discardCommand.argument)
            } else {
                userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
            }
        }
        discardCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun commandConfirmed() = if (configuration.application.password.promptOnRemoval) {
        userInterfaceAdapterPort
            .receiveConfirmation(
                outputOf(
                    shellOf(
                        """
                        Discarding an Egg is an irrevocable action.
                        Input 'c' to confirm or anything else to abort.
                        Your input: 
                        """.trimIndent(),
                    ),
                ),
            )
    } else {
        true
    }
}
