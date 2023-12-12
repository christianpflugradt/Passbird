package de.pflugradts.passbird.application.boot.main

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.commandhandling.InputHandler
import de.pflugradts.passbird.domain.model.nest.Nest
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.NestingGroundService

const val INTERRUPT = 0x03.toChar()

@Singleton
class PassbirdApplication @Inject constructor(
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    @Inject private val nestService: NestingGroundService,
    @Inject private val inputHandler: InputHandler,
) : Bootable {

    override fun boot() {
        userInterfaceAdapterPort.sendLineBreak()
        var input: Input
        while (!isSigTerm(receiveInput().also { input = it })) { inputHandler.handleInput(input) }
    }

    private fun receiveInput() = userInterfaceAdapterPort.receive(outputOf(shellOf(nestPrefix() + "Enter command: ")))

    private fun nestPrefix() = nestService.getCurrentNest().let {
        if (it == Nest.DEFAULT) "" else "[${it.shell.asString()}] "
    }

    private fun isSigTerm(input: Input) = input.data.isEmpty && !input.command.isEmpty && input.command.firstByte == INTERRUPT.code.toByte()
}
