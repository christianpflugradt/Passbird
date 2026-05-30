package de.pflugradts.passbird.application.commandhandling.handler.protein
import com.google.common.eventbus.Subscribe
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.DiscardProteinCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT
class DiscardProteinCommandHandler constructor(
    private val configuration: ReadableConfiguration,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : CommandHandler {
    @Subscribe
    private fun handleDiscardProteinCommand(discardProteinCommand: DiscardProteinCommand) {
        val eggIdShell = discardProteinCommand.argument
        val slot = discardProteinCommand.slot
        run {
            if (passwordService.eggExists(eggIdShell, CREATE_ENTRY_NOT_EXISTS_EVENT)) {
                if (!passwordService.proteinExists(eggIdShell, slot) || commandConfirmed(discardProteinCommand)) {
                    if (passwordService.discardProtein(eggIdShell, slot).failure) {
                        commandExecutionTracker.markFailure()
                    }
                } else {
                    commandExecutionTracker.markAborted()
                    userInterfaceAdapterPort.send(outputOf(shellOf("Operation aborted."), OPERATION_ABORTED))
                }
            } else {
                commandExecutionTracker.markFailure()
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
