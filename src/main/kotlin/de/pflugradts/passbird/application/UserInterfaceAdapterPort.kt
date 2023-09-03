package de.pflugradts.passbird.application

import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.model.transfer.Output

/**
 * AdapterPort for receiving [Input] from and sending [Output] to the user.
 */
interface UserInterfaceAdapterPort {
    fun receive(output: Output): Input
    fun receive(): Input = receive(Output.empty())
    fun receiveSecurely(output: Output): Input
    fun receiveSecurely(): Input = receiveSecurely(Output.empty())
    fun send(output: Output)
    fun sendLineBreak() = send(Output.empty())
    fun receiveConfirmation(output: Output): Boolean =
        receive(output).run { !isEmpty && data.isEmpty && command.firstByte == 'c'.code.toByte() }
}
