package de.pflugradts.passbird.application.commandhandling.handler.nest
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.AddNestCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.nest.NestService
class AddNestCommandHandler constructor(
    private val nestService: NestService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<AddNestCommand>(AddNestCommand::class.java) {
    override fun handleCommand(command: AddNestCommand) {
        if (command.slot == DEFAULT) {
            commandExecutionTracker.markAborted()
            userInterfaceAdapterPort.send(outputOf(shellOf("Default Nest cannot be replaced - Operation aborted."), OPERATION_ABORTED))
            return
        }
        val prompt = if (nestService.atNestSlot(command.slot).isPresent) {
            "Enter new name for existing Nest '${nestService.atNestSlot(command.slot).get().viewNestId().asString()}' " +
                "or nothing to abort\nYour input: "
        } else {
            "Enter name for Nest or nothing to abort\nYour input: "
        }
        val input = userInterfaceAdapterPort.receive(outputOf(shellOf(prompt)))
        if (input.isEmpty) {
            commandExecutionTracker.markAborted()
            userInterfaceAdapterPort.send(outputOf(shellOf("Empty input - Operation aborted."), OPERATION_ABORTED))
        } else {
            if (nestService.place(input.shell, command.slot).failure) {
                commandExecutionTracker.markFailure()
            }
        }
        input.invalidate()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
