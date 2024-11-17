package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.DiscardProteinCommand
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT

class DiscardProteinCommandHandler @Inject constructor(
    private val configuration: ReadableConfiguration,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleDiscardProteinCommand(discardProteinCommand: DiscardProteinCommand) {
        val eggIdShell = discardProteinCommand.argument
        val slot = discardProteinCommand.slot
        run {
            if (passwordService.eggExists(eggIdShell, CREATE_ENTRY_NOT_EXISTS_EVENT)) {
                if (!passwordService.proteinExists(eggIdShell, slot) || commandConfirmed(discardProteinCommand)) {
                    passwordService.discardProtein(eggIdShell, slot)
                } else {
                    userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
                }
            }
        }
        discardProteinCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun commandConfirmed(discardProteinCommand: DiscardProteinCommand) = if (configuration.application.password.promptOnRemoval &&
        passwordService.eggExists(discardProteinCommand.argument, PasswordService.EggNotExistsAction.DO_NOTHING)
    ) {
        userInterfaceAdapterPort
            .receiveConfirmation(
                outputOf(
                    shellOf(
                        """
                        Discarding a Protein is an irrevocable action.
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
