package de.pflugradts.passbird.application

import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.emptyOutput

interface UserInterfaceAdapterPort {
    fun receive(vararg output: Output): Input
    fun receive(): Input = receive(emptyOutput())
    fun receiveSecurely(output: Output): Input
    fun receiveSecurely(): Input = receiveSecurely(emptyOutput())
    fun receiveLineBreakWithin(milliseconds: Long): Boolean
    fun send(vararg output: Output)
    fun startEphemeralLine(output: Output)
    fun updateEphemeralLine(output: Output)
    fun finishEphemeralLine()
    fun sendLineBreak() = send(emptyOutput())
    fun receiveConfirmation(output: Output) =
        receive(output).run { !isEmpty && data.isEmpty && command.size == 1 && command.firstByte == 'c'.code.toByte() }

    fun receiveYes(output: Output) = receive(output).run {
        !isEmpty && data.isEmpty && command.size == 1 &&
            (command.firstByte == 'Y'.code.toByte() || command.firstByte == 'y'.code.toByte())
    }
    fun warningSound()
}
