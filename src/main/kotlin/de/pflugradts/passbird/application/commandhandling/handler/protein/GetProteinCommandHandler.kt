package de.pflugradts.passbird.application.commandhandling.handler.protein
import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.GetProteinCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.application.useScrambled
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting
import de.pflugradts.passbird.domain.service.password.PasswordService
class GetProteinCommandHandler constructor(
    private val passwordService: PasswordService,
    private val clipboardAdapterPort: ClipboardAdapterPort,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<GetProteinCommand>(GetProteinCommand::class.java) {
    override fun handleCommand(command: GetProteinCommand) {
        passwordService.viewProteinStructure(command.argument, command.slot).orNull()?.useScrambled {
            if (it.isNotEmpty) {
                val clipboardResult = clipboardAdapterPort.post(outputOf(it))
                if (clipboardResult.failure) {
                    commandExecutionTracker.markFailure()
                }
                clipboardResult.onSuccess {
                    userInterfaceAdapterPort.send(outputOf(shellOf("Protein copied to clipboard.")))
                }
            } else {
                commandExecutionTracker.markAborted()
                val msg = "Specified Protein Structure is empty - Operation aborted."
                userInterfaceAdapterPort.send(outputOf(shellOf(msg), OutputFormatting.OPERATION_ABORTED))
            }
        } ?: commandExecutionTracker.markFailure()
        command.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
