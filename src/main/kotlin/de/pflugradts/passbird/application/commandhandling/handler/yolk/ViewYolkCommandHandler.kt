package de.pflugradts.passbird.application.commandhandling.handler.yolk

import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.command.ViewYolkCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.yolk.TotpGenerator
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.OPERATION_ABORTED
import de.pflugradts.passbird.domain.service.password.PasswordService
import de.pflugradts.passbird.domain.service.password.PasswordService.EggNotExistsAction.CREATE_ENTRY_NOT_EXISTS_EVENT
import de.pflugradts.passbird.domain.service.password.YolkView

class ViewYolkCommandHandler(
    private val configuration: ReadableConfiguration,
    private val passwordService: PasswordService,
    private val clipboardAdapterPort: ClipboardAdapterPort,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val systemOperation: SystemOperation,
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

    private fun syncClipboard(code: String) {
        if (configuration.application.yolk.copyToClipboard) {
            clipboardAdapterPort.post(outputOf(shellOf(code)))
        }
    }

    private fun showYolk(yolkView: YolkView) {
        val secretBytes = yolkView.secret.toByteArray()
        try {
            val totpGenerator = TotpGenerator(systemOperation.clock)
            showCurrentCode(
                totpGenerator = totpGenerator,
                secretBytes = secretBytes,
                yolkView = yolkView,
            )
        } finally {
            secretBytes.fill(0)
            yolkView.secret.scramble()
        }
    }

    private fun showCurrentCode(totpGenerator: TotpGenerator, secretBytes: ByteArray, yolkView: YolkView) {
        var code = nextCode(totpGenerator, secretBytes, yolkView)
        userInterfaceAdapterPort.send(outputOf(shellOf("Press Enter to return.")))
        userInterfaceAdapterPort.sendLineBreak()
        syncClipboard(code.value)
        userInterfaceAdapterPort.startEphemeralLine(outputOf(shellOf("${code.value} (${code.remainingValiditySeconds}s)")))
        while (!userInterfaceAdapterPort.receiveLineBreakWithin(MILLI_SECONDS)) {
            val nextCode = nextCode(totpGenerator, secretBytes, yolkView)
            if (nextCode.value != code.value) {
                syncClipboard(nextCode.value)
            }
            code = nextCode
            userInterfaceAdapterPort.updateEphemeralLine(
                outputOf(shellOf("${code.value} (${code.remainingValiditySeconds}s)")),
            )
        }
        userInterfaceAdapterPort.finishEphemeralLine()
    }

    private fun nextCode(totpGenerator: TotpGenerator, secretBytes: ByteArray, yolkView: YolkView) = totpGenerator.generate(
        secret = secretBytes,
        algorithm = yolkView.algorithm,
        digits = yolkView.digits,
        periodSeconds = yolkView.periodSeconds,
    )

    private fun abortMissingYolk() {
        commandExecutionTracker.markAborted()
        userInterfaceAdapterPort.send(outputOf(shellOf("Yolk not found - Operation aborted."), OPERATION_ABORTED))
    }

    private fun finish(command: ViewYolkCommand) {
        command.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }

    companion object {
        private const val MILLI_SECONDS = 1000L
    }
}
