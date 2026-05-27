package de.pflugradts.passbird.adapter.userinterface

import de.pflugradts.passbird.application.InactivityTerminationRequestedException
import de.pflugradts.passbird.application.SecureInputUnavailableException
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.process.inactivity.InactivityTerminationSignal
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.PlainShell.Companion.plainShellOf
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting
import jakarta.inject.Inject
import jakarta.inject.Singleton

private const val INPUT_POLL_INTERVAL_IN_MILLIS = 50L

@Singleton
class CommandLineInterfaceService @Inject constructor(
    private val systemOperation: SystemOperation,
    private val configuration: ReadableConfiguration,
    private val inactivityTerminationSignal: InactivityTerminationSignal,
) : UserInterfaceAdapterPort {
    constructor(
        systemOperation: SystemOperation,
        configuration: ReadableConfiguration,
    ) : this(systemOperation, configuration, InactivityTerminationSignal())

    override fun receive(vararg output: Output) = output.forEach { sendWithoutLineBreak(it) }.run { receivePlain() }

    private fun receivePlain(): Input {
        val bytes = ArrayList<Byte>()
        var next: Char
        while (true) {
            if (hasPendingInput()) {
                if (isLinebreak(stdin().also { next = it })) {
                    return inputOf(shellOf(bytes))
                }
                if (!isCarriageReturn(next)) bytes.add(next.code.toByte())
            } else if (inactivityTerminationSignal.isRequested()) {
                throw InactivityTerminationRequestedException()
            } else {
                systemOperation.sleepMilliseconds(INPUT_POLL_INTERVAL_IN_MILLIS)
            }
        }
    }

    private fun hasPendingInput() = systemOperation.availableInputBytes() > 0

    private fun stdin(): Char = systemOperation.readCharFromStdin()
    private fun isLinebreak(chr: Char) = chr == '\n'
    private fun isCarriageReturn(chr: Char) = chr == '\r'

    override fun receiveSecurely(output: Output): Input {
        sendWithoutLineBreak(output)
        return when {
            !configuration.adapter.userInterface.secureInput -> receivePlain()
            systemOperation.isConsoleAvailable -> inputOf(plainShellOf(systemOperation.readPasswordFromConsole()).toShell())
            else -> throw SecureInputUnavailableException()
        }
    }

    override fun send(vararg output: Output) = output.forEach { sendWithoutLineBreak(it) }.also { sendChar('\n') }
    private fun sendWithoutLineBreak(vararg output: Output) = output.forEach {
        it.formatting?.also { formatting -> if (escapeCodesEnabled) beginEscape(formatting) }
        shellOf(it.shell.toByteArray()).forEach { char -> sendChar(Char(char.toUShort())) }
        it.formatting?.also { if (escapeCodesEnabled) endEscape() }
        it.formatting?.let { formatting -> if (formatting == OutputFormatting.OPERATION_ABORTED) warningSound() }
    }

    private fun sendChar(chr: Char) = print(chr)
    override fun warningSound() {
        if (audibleBell) sendChar('\u0007')
    }
    private val escapeCodesEnabled: Boolean get() = configuration.adapter.userInterface.ansiEscapeCodes.enabled
    private val audibleBell: Boolean get() = configuration.adapter.userInterface.audibleBell
}
