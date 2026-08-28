package de.pflugradts.passbird.application.yolk

import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.YolkView

class LiveYolkView(
    private val configuration: ReadableConfiguration,
    private val clipboardAdapterPort: ClipboardAdapterPort,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val systemOperation: SystemOperation,
) {
    fun show(yolkView: YolkView, awaitCompletion: (Long) -> Boolean) {
        val secretBytes = yolkView.secret.toByteArray()
        try {
            val totpGenerator = TotpGenerator(systemOperation.clock)
            var code = nextCode(totpGenerator, secretBytes, yolkView)
            syncClipboard(code.value)
            userInterfaceAdapterPort.startEphemeralLine(outputOf(shellOf("${code.value} (${code.remainingValiditySeconds}s)")))
            while (!awaitCompletion(MILLI_SECONDS)) {
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
        } finally {
            secretBytes.fill(0)
            yolkView.secret.scramble()
        }
    }

    private fun syncClipboard(code: String) {
        if (configuration.application.yolk.copyToClipboard) {
            clipboardAdapterPort.post(outputOf(shellOf(code)))
        }
    }

    private fun nextCode(totpGenerator: TotpGenerator, secretBytes: ByteArray, yolkView: YolkView) = totpGenerator.generate(
        secret = secretBytes,
        algorithm = yolkView.algorithm,
        digits = yolkView.digits,
        periodSeconds = yolkView.periodSeconds,
    )

    companion object {
        private const val MILLI_SECONDS = 1000L
    }
}
