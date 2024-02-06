package de.pflugradts.passbird.application.boot.main

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.application.Global
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.commandhandling.InputHandler
import de.pflugradts.passbird.application.process.Initializer
import de.pflugradts.passbird.application.process.inactivity.InactivityHandler
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.model.transfer.OutputFormatting.NEST
import de.pflugradts.passbird.domain.service.nest.NestingGroundService

const val INTERRUPT = 0x03.toChar()

@Singleton
class PassbirdApplication @Inject constructor(
    @Inject private val inactivityHandler: InactivityHandler,
    @Inject private val initializers: Set<Initializer>,
    @Inject private val inputHandler: InputHandler,
    @Inject private val nestService: NestingGroundService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : Bootable {

    override fun boot() {
        userInterfaceAdapterPort.sendLineBreak()
        nestService.moveToNestAt(Global.initialSlot)
        initializers.forEach { it.run() }
        var input: Input
        while (!isSigTerm(receiveInput().also { input = it })) {
            inactivityHandler.registerInteraction()
            inputHandler.handleInput(input)
        }
    }

    private fun receiveInput() = userInterfaceAdapterPort.receive(
        outputOf(shellOf(nestPrefix()), NEST),
        outputOf(shellOf("Enter command: ")),
    )

    private fun nestPrefix() = nestService.currentNest().let {
        if (it == Nest.DEFAULT) "" else "[${it.viewNestId().asString()}] "
    }

    private fun isSigTerm(input: Input) = input.data.isEmpty && !input.command.isEmpty && input.command.firstByte == INTERRUPT.code.toByte()
}
