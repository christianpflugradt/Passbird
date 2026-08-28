package de.pflugradts.passbird.application.commandhandling
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.InactivityTerminationRequestedException
import de.pflugradts.passbird.application.StdinTerminationRequestedException
import de.pflugradts.passbird.application.commandhandling.CommandType.Companion.resolveCommandTypeFrom
import de.pflugradts.passbird.application.commandhandling.command.NullCommand
import de.pflugradts.passbird.application.commandhandling.command.RepeatLastCommand
import de.pflugradts.passbird.application.commandhandling.command.UseCommand
import de.pflugradts.passbird.application.commandhandling.command.base.Command
import de.pflugradts.passbird.application.commandhandling.factory.CommandFactory
import de.pflugradts.passbird.application.failure.CommandFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.domain.model.transfer.Input
class CommandInputHandler constructor(
    private val commandBus: CommandBus,
    private val commandFactory: CommandFactory,
    private val rememberedCommandMemory: RememberedCommandMemory,
    private val commandExecutionTracker: CommandExecutionTracker,
) : InputHandler {
    override fun handleInput(input: Input) {
        val originalInput = input.shell.copy()
        commandExecutionTracker.begin()
        val command = tryCatching {
            commandFactory.construct(resolveCommandTypeFrom(input.command), input)
        }.onFailure {
            commandExecutionTracker.markFailure()
            reportFailure(CommandFailure(it))
        }.getOrNull()
        val outcome = if (command == null) {
            commandExecutionTracker.finish(CommandExecutionOutcome.FAILURE)
        } else {
            tryCatching {
                commandBus.post(command)
                commandExecutionTracker.finish(command.defaultOutcome())
            }.onFailure { ex ->
                if (ex is InactivityTerminationRequestedException || ex is StdinTerminationRequestedException) {
                    commandExecutionTracker.finish(CommandExecutionOutcome.FAILURE)
                    originalInput.scramble()
                    throw ex
                }
                commandExecutionTracker.markFailure()
                reportFailure(CommandFailure(ex))
            }.getOrNull() ?: commandExecutionTracker.finish(CommandExecutionOutcome.FAILURE)
        }
        if (shouldRemember(command, outcome)) {
            rememberedCommandMemory.remember(originalInput)
        } else {
            originalInput.scramble()
        }
    }
}

private fun shouldRemember(command: Command?, outcome: CommandExecutionOutcome) =
    command !is RepeatLastCommand && (outcome == CommandExecutionOutcome.SUCCESS || isRepeatableAbortedUse(command, outcome))

private fun isRepeatableAbortedUse(command: Command?, outcome: CommandExecutionOutcome) =
    command is UseCommand && outcome == CommandExecutionOutcome.ABORTED

private fun Command.defaultOutcome() = if (this is NullCommand) {
    CommandExecutionOutcome.FAILURE
} else {
    CommandExecutionOutcome.SUCCESS
}
