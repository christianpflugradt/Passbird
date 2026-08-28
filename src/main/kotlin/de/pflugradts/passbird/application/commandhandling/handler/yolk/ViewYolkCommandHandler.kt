package de.pflugradts.passbird.application.commandhandling.handler.yolk

import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.ViewYolkCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.application.yolk.LiveYolkView
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT
import de.pflugradts.passbird.domain.service.password.YolkView

class ViewYolkCommandHandler(
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val liveYolkView: LiveYolkView,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<ViewYolkCommand>(ViewYolkCommand::class.java) {
    override fun handleCommand(command: ViewYolkCommand) {
        if (!passwordService.eggExists(command.argument, CREATE_ENTRY_NOT_EXISTS_EVENT)) return finish(command)
        passwordService.viewYolk(command.argument).ifPresentOrElse(
            ::showYolk,
            ::abortMissingYolk,
        )
        finish(command)
    }

    private fun showYolk(yolkView: YolkView) {
        userInterfaceAdapterPort.send(outputOf(shellOf("Press Enter to return.")))
        userInterfaceAdapterPort.sendLineBreak()
        liveYolkView.show(yolkView, userInterfaceAdapterPort::receiveLineBreakWithin)
    }

    private fun abortMissingYolk() {
        commandExecutionTracker.markAborted()
        userInterfaceAdapterPort.send(outputOf(shellOf("Yolk not found - Operation aborted."), OPERATION_ABORTED))
    }

    private fun finish(command: ViewYolkCommand) {
        command.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
