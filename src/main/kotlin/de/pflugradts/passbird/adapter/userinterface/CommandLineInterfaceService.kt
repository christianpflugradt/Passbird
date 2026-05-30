package de.pflugradts.passbird.adapter.userinterface

import de.pflugradts.passbird.application.InactivityTerminationRequestedException
import de.pflugradts.passbird.application.SecureInputUnavailableException
import de.pflugradts.passbird.application.StdinTerminationRequestedException
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.process.inactivity.InactivityTerminationSignal
import de.pflugradts.passbird.domain.model.shell.PlainShell.Companion.plainShellOf
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

private const val INPUT_POLL_INTERVAL_IN_MILLIS = 50L
private val STDIN_EOF = Char.MAX_VALUE

@Singleton
class CommandLineInterfaceService @Inject constructor(
    private val terminalInputGateway: TerminalInputGateway,
    private val configuration: ReadableConfiguration,
    private val inactivityTerminationSignal: InactivityTerminationSignal,
) : UserInterfaceAdapterPort {
    private val inputReaderExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "passbird-input-reader").apply { isDaemon = true }
    }

    constructor(
        terminalInputGateway: TerminalInputGateway,
        configuration: ReadableConfiguration,
    ) : this(terminalInputGateway, configuration, InactivityTerminationSignal())

    override fun receive(vararg output: Output) = output.forEach { sendWithoutLineBreak(it) }.run { receivePlain() }

    private fun receivePlain(): Input {
        val bytes = ArrayList<Byte>()
        while (true) {
            val next = readCharFromVisibleStdin()
            if (isEndOfInput(next)) {
                return bytes.toInputOrThrow()
            }
            if (isLinebreak(next)) {
                return inputOf(shellOf(bytes))
            }
            if (!isCarriageReturn(next)) bytes.add(next.code.toByte())
        }
    }

    private fun stdin(): Char = terminalInputGateway.readCharFromStdin()
    private fun isLinebreak(chr: Char) = chr == '\n'
    private fun isCarriageReturn(chr: Char) = chr == '\r'
    private fun isEndOfInput(chr: Char) = chr == STDIN_EOF

    private fun readCharFromVisibleStdin(): Char = readWithInactivityCheck { runCatching(::stdin).getOrDefault(STDIN_EOF) }

    private fun readPasswordFromConsole(): CharArray = readWithInactivityCheck { terminalInputGateway.readPasswordFromConsole() }

    private fun <T> readWithInactivityCheck(read: () -> T): T {
        if (inactivityTerminationSignal.isRequested()) {
            throw InactivityTerminationRequestedException()
        }
        val readTask = inputReaderExecutor.submit<T> { read() }
        while (true) {
            try {
                return readTask.get(INPUT_POLL_INTERVAL_IN_MILLIS, TimeUnit.MILLISECONDS)
            } catch (ex: ExecutionException) {
                throw ex.cause ?: ex
            } catch (_: TimeoutException) {
                if (inactivityTerminationSignal.isRequested()) {
                    readTask.cancel(true)
                    throw InactivityTerminationRequestedException()
                }
            }
        }
    }

    private fun List<Byte>.toInputOrThrow(): Input = takeIf { it.isNotEmpty() }?.let { inputOf(shellOf(it)) }
        ?: throw StdinTerminationRequestedException()

    override fun receiveSecurely(output: Output): Input {
        sendWithoutLineBreak(output)
        return when {
            !configuration.adapter.userInterface.secureInput -> receivePlain()
            terminalInputGateway.isConsoleAvailable -> readPasswordFromConsole().toInput()
            else -> throw SecureInputUnavailableException()
        }
    }

    override fun send(vararg output: Output) = output.forEach { sendWithoutLineBreak(it) }.also { sendChar('\n') }
    private fun sendWithoutLineBreak(vararg output: Output) = output.forEach {
        it.formatting?.also { formatting -> if (escapeCodesEnabled) beginEscape(formatting) }
        val renderedShell = it.shell.copy()
        try {
            renderedShell.forEach { char -> sendChar(Char(char.toUShort())) }
        } finally {
            renderedShell.scramble()
        }
        it.formatting?.also { if (escapeCodesEnabled) endEscape() }
        it.formatting?.let { formatting -> if (formatting == OutputFormatting.OPERATION_ABORTED) warningSound() }
    }

    private fun CharArray.toInput(): Input = try {
        inputOf(plainShellOf(this).toShell())
    } finally {
        fill(Char.MIN_VALUE)
    }

    private fun sendChar(chr: Char) = print(chr)
    override fun warningSound() {
        if (audibleBell) sendChar('\u0007')
    }
    private val escapeCodesEnabled: Boolean get() = configuration.adapter.userInterface.ansiEscapeCodes.enabled
    private val audibleBell: Boolean get() = configuration.adapter.userInterface.audibleBell
}
