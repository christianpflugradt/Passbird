package de.pflugradts.passbird.application.commandhandling.handler.nest

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.AddNestCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.nest.NestSlot.DEFAULT
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.NestService

class AddNestCommandHandler @Inject constructor(
    @Inject private val nestService: NestService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleAddNestCommand(addNestCommand: AddNestCommand) {
        if (addNestCommand.nestSlot == DEFAULT) {
            userInterfaceAdapterPort.send(outputOf(shellOf("Default namespace cannot be replaced - Operation aborted.")))
            return
        }
        val prompt = if (nestService.atNestSlot(addNestCommand.nestSlot).isPresent) {
            "Enter new name for existing namespace '${nestService.atNestSlot(addNestCommand.nestSlot).get().shell.asString()}' " +
                "or nothing to abort%nYour input: "
        } else {
            "Enter name for namespace or nothing to abort\nYour input: "
        }
        val input = userInterfaceAdapterPort.receive(outputOf(shellOf(prompt)))
        if (input.isEmpty) {
            userInterfaceAdapterPort.send(outputOf(shellOf("Empty input - Operation aborted.")))
        } else {
            nestService.place(input.shell, addNestCommand.nestSlot)
        }
        input.invalidate()
        userInterfaceAdapterPort.sendLineBreak()
    }
}