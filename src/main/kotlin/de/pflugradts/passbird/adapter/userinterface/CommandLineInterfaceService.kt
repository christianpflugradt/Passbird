package de.pflugradts.passbird.adapter.userinterface

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.PlainShell.Companion.plainShellOf
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting

@Singleton
class CommandLineInterfaceService @Inject constructor(
    private val systemOperation: SystemOperation,
    private val configuration: ReadableConfiguration,
) : UserInterfaceAdapterPort {

    override fun receive(vararg output: Output) = output.forEach { sendWithoutLineBreak(it) }.run { receivePlain() }

    private fun receivePlain(): Input {
        val bytes = ArrayList<Byte>()
        var next: Char
        while (!isLinebreak(stdin().also { next = it })) bytes.add(next.code.toByte())
        return inputOf(shellOf(bytes))
    }

    private fun stdin(): Char = System.`in`.read().toChar()
    private fun isLinebreak(chr: Char) = chr == '\n'

    override fun receiveSecurely(output: Output): Input {
        sendWithoutLineBreak(output)
        return if (configuration.adapter.userInterface.secureInput && systemOperation.isConsoleAvailable) {
            inputOf(plainShellOf(systemOperation.readPasswordFromConsole()).toShell())
        } else {
            receive()
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
