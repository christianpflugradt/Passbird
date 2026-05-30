package de.pflugradts.passbird.application.commandhandling.handler.nest
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.SwitchNestCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.nest.NestService
class SwitchNestCommandHandler constructor(
    private val nestService: NestService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<SwitchNestCommand>(SwitchNestCommand::class.java) {
    override fun handleCommand(command: SwitchNestCommand) {
        if (nestService.currentNest().slot == command.slot) {
            userInterfaceAdapterPort.send(
                outputOf(shellOf("'${nestService.currentNest().viewNestId().asString()}' is already the current Nest.")),
            )
        } else if (nestService.atNestSlot(command.slot).isPresent) {
            nestService.moveToNestAt(command.slot)
        } else {
            commandExecutionTracker.markAborted()
            userInterfaceAdapterPort.send(outputOf(shellOf("Specified Nest does not exist - Operation aborted."), OPERATION_ABORTED))
        }
        userInterfaceAdapterPort.sendLineBreak()
    }
}
