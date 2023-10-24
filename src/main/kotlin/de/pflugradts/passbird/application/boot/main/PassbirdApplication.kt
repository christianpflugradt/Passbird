package de.pflugradts.passbird.application.boot.main

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.commandhandling.InputHandler
import de.pflugradts.passbird.domain.model.namespace.Namespace
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
import de.pflugradts.passbird.domain.model.transfer.Input
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import de.pflugradts.passbird.domain.service.FixedNamespaceService

const val INTERRUPT = 0x03.toChar()

@Singleton
class PassbirdApplication @Inject constructor(
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    @Inject private val namespaceService: FixedNamespaceService,
    @Inject private val inputHandler: InputHandler,
) : Bootable {

    override fun boot() {
        userInterfaceAdapterPort.sendLineBreak()
        var input: Input
        while (!isSigTerm(receiveInput().also { input = it })) { inputHandler.handleInput(input) }
    }

    private fun receiveInput() = userInterfaceAdapterPort.receive(outputOf(bytesOf(namespacePrefix() + "Enter command: ")))

    private fun namespacePrefix() = namespaceService.getCurrentNamespace().let {
        if (it == Namespace.DEFAULT) "" else "[${it.bytes.asString()}] "
    }

    private fun isSigTerm(input: Input) = input.data.isEmpty && !input.command.isEmpty && input.command.firstByte == INTERRUPT.code.toByte()
}
