package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.ListCommand
import de.pflugradts.passbird.application.util.copyBytes
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService

class ListCommandHandler @Inject constructor(
    @Inject private val passwordService: PasswordService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleListCommand(@Suppress("UNUSED_PARAMETER") listCommand: ListCommand) {
        userInterfaceAdapterPort.send(outputOf(join(passwordService.findAllEggIds().toList())))
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun join(eggIdShellList: List<Shell>) = if (eggIdShellList.isEmpty()) {
        shellOf("database is empty")
    } else {
        val count = eggIdShellList.stream().map(Shell::size)
            .reduce((eggIdShellList.size - 1) * 2) { a: Int, b: Int -> Integer.sum(a, b) }
        val bytes = ByteArray(count)
        var index = 0
        eggIdShellList.forEach {
            copyBytes(it.toByteArray(), bytes, index, it.size)
            index += it.size
            if (index < count) {
                bytes[index++] = ','.code.toByte()
                bytes[index++] = ' '.code.toByte()
            }
        }
        shellOf(bytes)
    }
}
