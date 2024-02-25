package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.kotlinextensions.Option
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.ViewProteinTypesCommand
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.password.PasswordService

class ViewProteinTypesCommandHandler @Inject constructor(
    @Inject private val canPrintInfo: CanPrintInfo,
    @Inject private val passwordService: PasswordService,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleViewProteinTypesCommand(viewProteinTypesCommand: ViewProteinTypesCommand) {
        passwordService.viewProteinTypes(viewProteinTypesCommand.argument).orNull()?.also {
            userInterfaceAdapterPort.send(*outputsOfHeader())
            it.forEachIndexed { index, proteinType -> userInterfaceAdapterPort.send(*outputsOf(index, proteinType)) }
        }
        viewProteinTypesCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun outputsOfHeader(): Array<Output> = with(canPrintInfo) {
        arrayOf(out("\n"), outBold(SLOT_HEADER), outBold("Type".padded()), outBold(SEP), outBold("Structure".padded()))
    }
    private fun outputsOf(index: Int, proteinType: Option<Shell>): Array<Output> = with(canPrintInfo) {
        arrayOf(
            out(padded(index)),
            out(proteinType.map { it.asString() }.orElse("---").padded()),
            out(SEP),
            out(proteinType.map { "******" }.orElse("---").padded()),
        )
    }
}

private const val SEP = " | "
private const val SLOT_HEADER = "Slot "
private fun String.padded() = padEnd(20, ' ')
private fun padded(index: Int) = "$index:".padEnd(SLOT_HEADER.length, ' ')
