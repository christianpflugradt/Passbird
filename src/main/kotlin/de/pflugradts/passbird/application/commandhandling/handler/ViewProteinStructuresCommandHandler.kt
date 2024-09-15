package de.pflugradts.passbird.application.commandhandling.handler

import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import de.pflugradts.kotlinextensions.MutableOption.Companion.emptyOption
import de.pflugradts.kotlinextensions.MutableOption.Companion.optionOf
import de.pflugradts.kotlinextensions.Option
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.ViewProteinStructuresCommand
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.ShellPair
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.password.PasswordService

class ViewProteinStructuresCommandHandler @Inject constructor(
    private val canPrintInfo: CanPrintInfo,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : CommandHandler {
    @Subscribe
    private fun handleViewProteinStructuresCommand(viewProteinStructuresCommand: ViewProteinStructuresCommand) {
        passwordService.viewProteinTypes(viewProteinStructuresCommand.argument).orNull()?.also { types ->
            passwordService.viewProteinStructures(viewProteinStructuresCommand.argument).orNull()?.also { structures ->
                userInterfaceAdapterPort.send(*outputsOfHeader())
                types.zip(structures).forEachIndexed { index, proteinPair ->
                    userInterfaceAdapterPort.send(*outputsOf(index, proteinPair.toShellPairOption()))
                }
            }
        }
        viewProteinStructuresCommand.invalidateInput()
        userInterfaceAdapterPort.sendLineBreak()
    }

    private fun outputsOfHeader(): Array<Output> = with(canPrintInfo) {
        arrayOf(out("\n"), outBold(SLOT_HEADER), outBold("Type".padded()), outBold(SEP), outBold("Structure".padded()))
    }
    private fun outputsOf(index: Int, shellPairOption: Option<ShellPair>): Array<Output> = with(canPrintInfo) {
        arrayOf(
            out(padded(index)),
            out(shellPairOption.map { it.first.asString() }.orElse("---").padded()),
            out(SEP),
            out(shellPairOption.map { it.second.asString() }.orElse("---").padded()),
        )
    }
}

private const val SEP = " | "
private const val SLOT_HEADER = "Slot "
private fun String.padded() = padEnd(20, ' ')
private fun padded(index: Int) = "$index:".padEnd(SLOT_HEADER.length, ' ')
private fun Pair<Option<Shell>, Option<Shell>>.toShellPairOption(): Option<ShellPair> =
    first.map { second.map { optionOf(first.get() to second.get()) }.orElse(emptyOption()) }.orElse(emptyOption())
