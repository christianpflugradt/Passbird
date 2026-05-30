package de.pflugradts.passbird.application.commandhandling.handler.protein
import de.pflugradts.kotlinextensions.Option
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.CommandExecutionTracker
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.ViewProteinTypesCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.transfer.Output
import de.pflugradts.passbird.domain.service.password.PasswordService
class ViewProteinTypesCommandHandler constructor(
    private val canPrintInfo: CanPrintInfo,
    private val passwordService: PasswordService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val commandExecutionTracker: CommandExecutionTracker,
) : TypedCommandHandler<ViewProteinTypesCommand>(ViewProteinTypesCommand::class.java) {
    override fun handleCommand(command: ViewProteinTypesCommand) {
        passwordService.viewProteinTypes(command.argument).orNull()?.also { proteinTypes ->
            try {
                userInterfaceAdapterPort.send(*outputsOfHeader())
                proteinTypes.forEachIndexed { index, proteinType -> userInterfaceAdapterPort.send(*outputsOf(index, proteinType)) }
            } finally {
                proteinTypes.forEach { it.ifPresent(Shell::scramble) }
            }
        } ?: commandExecutionTracker.markFailure()
        command.invalidateInput()
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
            out(proteinType.map { "*****" }.orElse("---").padded()),
        )
    }
}
private const val SEP = " | "
private const val SLOT_HEADER = "Slot "
private fun String.padded() = padEnd(20, ' ')
private fun padded(index: Int) = "$index:".padEnd(SLOT_HEADER.length, ' ')
