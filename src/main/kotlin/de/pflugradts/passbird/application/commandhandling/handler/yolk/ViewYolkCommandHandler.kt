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

class ViewYolkCommandHandler(
    private val configuration: ReadableConfiguration,
    private val passwordService: PasswordService,
    private val clipboardAdapterPort: ClipboardAdapterPort,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val systemOperation: SystemOperation,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<ViewYolkCommand>(ViewYolkCommand::class.java) {
    override fun handleCommand(command: ViewYolkCommand) {
        passwordService.viewYolk(command.argument).ifPresentOrElse(
            { yolkView ->
                val secretBytes = yolkView.secret.toByteArray()
                try {
                    val totpGenerator = TotpGenerator(systemOperation.clock)
                    var code = totpGenerator.generate(
                        secret = secretBytes,
                        algorithm = yolkView.algorithm,
                        digits = yolkView.digits,
                        periodSeconds = yolkView.periodSeconds,
                    )
                    if (code.remainingValiditySeconds <= minimumValiditySeconds) {
                        userInterfaceAdapterPort.send(
                            outputOf(
                                shellOf("Current Yolk expires in ${code.remainingValiditySeconds}s. Waiting for next Yolk..."),
                            ),
                        )
                        systemOperation.sleep(code.remainingValiditySeconds * MILLI_SECONDS)
                        code = totpGenerator.generate(
                            secret = secretBytes,
                            algorithm = yolkView.algorithm,
                            digits = yolkView.digits,
                            periodSeconds = yolkView.periodSeconds,
                        )
                    }
                    userInterfaceAdapterPort.send(outputOf(shellOf("Press Enter to return.")))
                    userInterfaceAdapterPort.sendLineBreak()
                    syncClipboard(code.value)
                    userInterfaceAdapterPort.startEphemeralLine(outputOf(shellOf("${code.value} (${code.remainingValiditySeconds}s)")))
                    while (!userInterfaceAdapterPort.receiveLineBreakWithin(MILLI_SECONDS)) {
                        val nextCode = totpGenerator.generate(
                            secret = secretBytes,
                            algorithm = yolkView.algorithm,
                            digits = yolkView.digits,
                            periodSeconds = yolkView.periodSeconds,
                        )
                        if (nextCode.value != code.value) {
                            syncClipboard(nextCode.value)
                        }
                        code = nextCode
                        userInterfaceAdapterPort.updateEphemeralLine(
                            outputOf(shellOf("${code.value} (${code.remainingValiditySeconds}s)")),
                        )
                    }
                    userInterfaceAdapterPort.finishEphemeralLine()
                } finally {
                    secretBytes.fill(0)
                    yolkView.secret.scramble()
                }
            },
            {
                commandExecutionTracker.markAborted()
                userInterfaceAdapterPort.send(outputOf(shellOf("Yolk not found - Operation aborted."), OPERATION_ABORTED))
            },
        )
        command.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun syncClipboard(code: String) {
        if (configuration.application.yolk.copyToClipboard) {
            clipboardAdapterPort.post(outputOf(shellOf(code)))
        }
    }

    companion object {
        private const val MINIMUM_VALIDITY_SECONDS = 5
        private const val MILLI_SECONDS = 1000L
    }

    private val minimumValiditySeconds get() = MINIMUM_VALIDITY_SECONDS
}
