package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.DiscardCommand
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService

class DiscardCommandHandler @Inject constructor(
    @Inject private val configuration: ReadableConfiguration,
    @Inject private val passwordService: PasswordService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleDiscardCommand(discardCommand: DiscardCommand) {
        if (commandConfirmed()) {
            passwordService.discardPasswordEntry(discardCommand.argument)
        } else {
            userInterfaceAdapterPort.send(outputOf(bytesOf("Operation aborted.")))
        }
        discardCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun commandConfirmed() =
        if (configuration.application.password.promptOnRemoval) {
            userInterfaceAdapterPort
                .receiveConfirmation(
                    outputOf(
                        bytesOf(
                            """
                            Discarding a Password Entry is an irrevocable action.
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
