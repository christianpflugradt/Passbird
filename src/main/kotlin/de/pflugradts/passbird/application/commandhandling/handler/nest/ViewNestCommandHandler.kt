package de.pflugradts.passbird.application.commandhandling.handler.nest
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.commandhandling.capabilities.CanListAvailableNests
import de.pflugradts.passbird.application.commandhandling.capabilities.CanPrintInfo
import de.pflugradts.passbird.application.commandhandling.command.ViewNestCommand
import de.pflugradts.passbird.application.commandhandling.handler.TypedCommandHandler
import de.pflugradts.passbird.application.commandhandling.handler.commandInfoOutputs
import de.pflugradts.passbird.domain.service.nest.NestService
class ViewNestCommandHandler constructor(
    private val canPrintInfo: CanPrintInfo,
    private val canListAvailableNests: CanListAvailableNests,
    private val nestService: NestService,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
) : TypedCommandHandler<ViewNestCommand>(ViewNestCommand::class.java) {
    override fun handleCommand(@Suppress("UNUSED_PARAMETER") command: ViewNestCommand) {
        with(canPrintInfo) {
            userInterfaceAdapterPort.send(
                *buildList {
                    add(outBold("\nCurrent Nest: "))
                    add(out(currentNest))
                    add(outBold("\n\nAvailable Nests:\n"))
                    add(out(availableNests))
                    add(outBold("\n\nAvailable Nest commands:\n"))
                    addAll(
                        commandInfoOutputs("n", " (view)       Displays the current Nest, available Nests, and related commands.").toList(),
                    )
                    addAll(commandInfoOutputs("n0", " (switch)     Switches to the default Nest.").toList())
                    addAll(commandInfoOutputs("n[1-9]", " (switch)     Switches to the Nest in the specified Nest Slot (1–9).").toList())
                    addAll(
                        commandInfoOutputs(
                            "n[EggId]",
                            " (assign)     Assigns the specified EggId to a Nest selected interactively.",
                        ).toList(),
                    )
                    addAll(commandInfoOutputs("n+[1-9]", " (create)     Creates a new Nest in the specified Nest Slot.").toList())
                    addAll(commandInfoOutputs("n-[1-9]", " (discard)    Deletes the Nest in the specified Nest Slot.").toList())
                    add(out("\n"))
                }.toTypedArray(),
            )
        }
    }
    private val currentNest get() = nestService.currentNest().viewNestId().asString()
    private val availableNests get() = canListAvailableNests.getAvailableNests(includeCurrent = true).let {
        if (canListAvailableNests.hasCustomNests()) it else "$it\n  (use the n+ command to create custom Nests)\n"
    }
}
