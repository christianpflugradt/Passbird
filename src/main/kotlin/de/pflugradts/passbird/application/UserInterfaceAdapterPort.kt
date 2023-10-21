package de.pflugradts.passbird.application

import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.emptyOutput

/**
 * AdapterPort for receiving [Input] from and sending [Output] to the user.
 */
interface UserInterfaceAdapterPort {
    fun receive(output: Output): Input
    fun receive(): Input = receive(emptyOutput())
    fun receiveSecurely(output: Output): Input
    fun receiveSecurely(): Input = receiveSecurely(emptyOutput())
    fun send(output: Output)
    fun sendLineBreak() = send(emptyOutput())
    fun receiveConfirmation(output: Output) = receive(output).run { !isEmpty && data.isEmpty && command.firstByte == 'c'.code.toByte() }
}
