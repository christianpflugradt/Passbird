package de.pflugradts.passbird.application

import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.emptyOutput

interface UserInterfaceAdapterPort {
    fun receive(vararg output: Output): Input
    fun receive(): Input = receive(emptyOutput())
    fun receiveSecurely(output: Output): Input
    fun receiveSecurely(): Input = receiveSecurely(emptyOutput())
    fun send(vararg output: Output)
    fun sendLineBreak() = send(emptyOutput())
    fun receiveConfirmation(output: Output) = receive(output).run { !isEmpty && data.isEmpty && command.firstByte == 'c'.code.toByte() }
    fun receiveYes(output: Output) = receive(output).run { !isEmpty && data.isEmpty && command.firstByte == 'Y'.code.toByte() }
    fun warningSound()
}
