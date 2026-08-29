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
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
private const val INPUT_POLL_INTERVAL_IN_MILLIS = 50L
private val STDIN_EOF = Char.MAX_VALUE

class CommandLineInterfaceService constructor(
    private val terminalInputGateway: TerminalInputGateway,
    private val configuration: ReadableConfiguration,
    private val inactivityTerminationSignal: InactivityTerminationSignal,
) : UserInterfaceAdapterPort {
    private val inputReaderExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "passbird-input-reader").apply { isDaemon = true }
    }
    private var visibleStdinReadTask: Future<Char>? = null
    private var ephemeralLineLength = 0
    private val nextActionBuffer = StringBuilder()

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
    private fun readCharFromVisibleStdin(): Char {
        val readTask = visibleStdinReadTask ?: inputReaderExecutor.submit<Char> {
            runCatching(::stdin).getOrDefault(STDIN_EOF)
        }.also { visibleStdinReadTask = it }
        while (true) {
            if (inactivityTerminationSignal.isRequested()) {
                throw InactivityTerminationRequestedException()
            }
            try {
                return readTask.get(INPUT_POLL_INTERVAL_IN_MILLIS, TimeUnit.MILLISECONDS).also {
                    visibleStdinReadTask = null
                }
            } catch (ex: ExecutionException) {
                visibleStdinReadTask = null
                throw ex.cause ?: ex
            } catch (_: TimeoutException) {
                continue
            }
        }
    }
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

    override fun receiveLineBreakWithin(milliseconds: Long): Boolean = receiveNextActionWithin(emptySet(), milliseconds)

    override fun receiveNextActionWithin(key: Char, milliseconds: Long): Boolean =
        receiveNextActionWithin(expectedNextActionSequences(key), milliseconds)

    private fun receiveNextActionWithin(expectedSequences: Set<String>, milliseconds: Long): Boolean {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(milliseconds)
        while (true) {
            if (inactivityTerminationSignal.isRequested()) {
                throw InactivityTerminationRequestedException()
            }
            val remainingNanos = deadline - System.nanoTime()
            if (remainingNanos <= 0L) {
                return false
            }
            val next = readCharFromVisibleStdinWithin(
                TimeUnit.NANOSECONDS.toMillis(remainingNanos).coerceAtLeast(1L),
            ) ?: return false
            if (isEndOfInput(next) || isLinebreak(next) || isCarriageReturn(next)) {
                nextActionBuffer.clear()
                return true
            }
            if (expectedSequences.isEmpty()) {
                continue
            }
            nextActionBuffer.append(next)
            if (expectedSequences.any { nextActionBuffer.endsWith(it) }) {
                nextActionBuffer.clear()
                return true
            }
            trimNextActionBuffer(expectedSequences)
        }
    }

    override fun send(vararg output: Output) = output.forEach { sendWithoutLineBreak(it) }.also { sendChar('\n') }

    override fun startEphemeralLine(output: Output) {
        ephemeralLineLength = renderWithoutLineBreak(output)
    }

    override fun updateEphemeralLine(output: Output) {
        sendChar('\r')
        val nextLength = renderWithoutLineBreak(output)
        if (nextLength < ephemeralLineLength) {
            repeat(ephemeralLineLength - nextLength) { sendChar(' ') }
            sendChar('\r')
            renderWithoutLineBreak(output)
        }
        ephemeralLineLength = nextLength
    }

    override fun finishEphemeralLine() {
        ephemeralLineLength = 0
        sendChar('\n')
    }

    private fun sendWithoutLineBreak(vararg output: Output) = output.forEach {
        renderOutput(it)
    }

    private fun renderWithoutLineBreak(output: Output): Int = renderOutput(output)

    private fun renderOutput(output: Output): Int {
        var renderedLength = 0
        output.formatting?.also { formatting -> if (escapeCodesEnabled) beginEscape(formatting) }
        val renderedShell = output.shell.copy()
        try {
            for (index in 0 until renderedShell.size) {
                sendChar(renderedShell.getChar(index))
                renderedLength++
            }
        } finally {
            renderedShell.scramble()
        }
        output.formatting?.also { if (escapeCodesEnabled) endEscape() }
        output.formatting?.let { formatting -> if (formatting == OutputFormatting.OPERATION_ABORTED) warningSound() }
        return renderedLength
    }

    private fun readCharFromVisibleStdinWithin(timeoutInMillis: Long): Char? {
        val readTask = visibleStdinReadTask ?: inputReaderExecutor.submit<Char> {
            runCatching(::stdin).getOrDefault(STDIN_EOF)
        }.also { visibleStdinReadTask = it }
        return try {
            readTask.get(timeoutInMillis, TimeUnit.MILLISECONDS).also { visibleStdinReadTask = null }
        } catch (ex: ExecutionException) {
            visibleStdinReadTask = null
            throw ex.cause ?: ex
        } catch (_: TimeoutException) {
            null
        }
    }
    private fun CharArray.toInput(): Input = try {
        inputOf(plainShellOf(this).toShell())
    } finally {
        fill(Char.MIN_VALUE)
    }
    private fun expectedNextActionSequences(key: Char) = setOf(
        "\u001b[${key.lowercaseChar().code};6u",
        "\u001b[${key.uppercaseChar().code};6u",
    )
    private fun trimNextActionBuffer(expectedSequences: Set<String>) {
        val buffer = nextActionBuffer.toString()
        val suffix = buffer.suffixMatchingAnyPrefixOf(expectedSequences)
        nextActionBuffer.clear()
        nextActionBuffer.append(suffix)
    }

    private fun StringBuilder.endsWith(suffix: String): Boolean = length >= suffix.length && substring(length - suffix.length) == suffix

    private fun String.suffixMatchingAnyPrefixOf(expectedSequences: Set<String>): String = prefixesOf(expectedSequences)
        .filter { endsWith(it) }
        .maxByOrNull(String::length)
        .orEmpty()

    private fun prefixesOf(expectedSequences: Set<String>) = expectedSequences
        .flatMap { sequence -> (1..sequence.length).map(sequence::take) }
        .toSet()
    private fun sendChar(chr: Char) = print(chr)
    override fun warningSound() {
        if (audibleBell) sendChar('\u0007')
    }
    private val escapeCodesEnabled: Boolean get() = configuration.adapter.userInterface.ansiEscapeCodes.enabled
    private val audibleBell: Boolean get() = configuration.adapter.userInterface.audibleBell
}
