package de.pflugradts.passbird.application.commandhandling.handler.memory

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.ViewMemoryCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.password.PasswordService

class ViewMemoryCommandHandler @Inject constructor(
    private val canPrintInfo: CanPrintInfo,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleViewMemoryCommand(viewMemoryCommand: ViewMemoryCommand) {
        val memory = passwordService.viewMemory()
        userInterfaceAdapterPort.sendLineBreak()
        with(canPrintInfo) {
            memory[0].ifPresentOrElse(
                block = {
                    memory.takeWhile { it.isPresent }.forEachIndexed { index, mutableOption ->
                        userInterfaceAdapterPort.send(outBold("$index:"), out(" ${mutableOption.get().asString()}"))
                    }
                },
                other = { userInterfaceAdapterPort.send(outputOf(shellOf("EggIdMemory is empty."))) },
            )
        }
        userInterfaceAdapterPort.sendLineBreak()
    }
}
