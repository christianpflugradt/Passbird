package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.ListCommand
import de.pflugradts.passbird.application.util.copyBytes
import de.pflugradts.passbird.domain.model.transfer.Bytes
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService

class ListCommandHandler @Inject constructor(
    @Inject private val passwordService: PasswordService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleListCommand(listCommand: ListCommand) {
        userInterfaceAdapterPort.send(outputOf(join(passwordService.findAllKeys().toList())))
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun join(keyBytesList: List<Bytes>) =
        if (keyBytesList.isEmpty()) {
            bytesOf("database is empty")
        } else {
            val count = keyBytesList.stream()
                .map(Bytes::size)
                .reduce((keyBytesList.size - 1) * 2) { a: Int, b: Int -> Integer.sum(a, b) }
            val bytes = ByteArray(count)
            var index = 0
            keyBytesList.forEach {
                copyBytes(it.toByteArray(), bytes, index, it.size)
                index += it.size
                if (index < count) {
                    bytes[index++] = ','.code.toByte()
                    bytes[index++] = ' '.code.toByte()
                }
            }
            bytesOf(bytes)
        }
}
