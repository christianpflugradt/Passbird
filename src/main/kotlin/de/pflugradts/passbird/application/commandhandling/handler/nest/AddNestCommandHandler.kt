package de.pflugradts.passbird.application.commandhandling.handler.nest

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.command.AddNestCommand
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import de.pflugradts.passbird.domain.model.nest.Slot.DEFAULT
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.NestService

class AddNestCommandHandler @Inject constructor(
    @Inject private val nestService: NestService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleAddNestCommand(addNestCommand: AddNestCommand) {
        if (addNestCommand.slot == DEFAULT) {
            userInterfaceAdapterPort.send(outputOf(bytesOf("Default namespace cannot be replaced - Operation aborted.")))
            return
        }
        val prompt = if (nestService.atSlot(addNestCommand.slot).isPresent) {
            "Enter new name for existing namespace '${nestService.atSlot(addNestCommand.slot).get().bytes.asString()}' " +
                "or nothing to abort%nYour input: "
        } else {
            "Enter name for namespace or nothing to abort\nYour input: "
        }
        val input = userInterfaceAdapterPort.receive(outputOf(bytesOf(prompt)))
        if (input.isEmpty) {
            userInterfaceAdapterPort.send(outputOf(bytesOf("Empty input - Operation aborted.")))
        } else {
            nestService.deploy(input.bytes, addNestCommand.slot)
        }
        input.invalidate()
        userInterfaceAdapterPort.sendLineBreak()
    }
}
