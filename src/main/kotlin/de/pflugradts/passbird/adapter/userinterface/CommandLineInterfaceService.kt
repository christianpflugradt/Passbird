package de.pflugradts.passbird.adapter.userinterface

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Chars
import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.model.transfer.Output

@Singleton
class CommandLineInterfaceService @Inject constructor(
    @Inject private val systemOperation: SystemOperation,
    @Inject private val configuration: ReadableConfiguration,
) : UserInterfaceAdapterPort {

    override fun receive(output: Output): Input {
        sendWithoutLineBreak(output)
        return receivePlain()
    }

    private fun receivePlain(): Input {
        val bytes = ArrayList<Byte>()
        var next: Char
        while (!isLinebreak(stdin().also { next = it })) { bytes.add(next.code.toByte()) }
        return Input.of(Bytes.of(bytes))
    }

    private fun stdin(): Char = System.`in`.read().toChar()
    private fun isLinebreak(chr: Char) = chr == '\n'

    override fun receiveSecurely(output: Output): Input {
        sendWithoutLineBreak(output)
        return if (configuration.getAdapter().getUserInterface().isSecureInput() && systemOperation.isConsoleAvailable) {
            Input.of(Chars.of(systemOperation.readPasswordFromConsole()).toBytes())
        } else {
            receive()
        }
    }

    private fun sendWithoutLineBreak(output: Output) = Bytes.of(*output.getBytes().toByteArray()).forEach { sendChar(Char(it.toUShort())) }

    private fun sendWithLineBreak(output: Output) {
        sendWithoutLineBreak(output)
        sendChar('\n')
    }

    private fun sendChar(chr: Char) = print(chr)
    override fun send(output: Output) = sendWithLineBreak(output)
}
