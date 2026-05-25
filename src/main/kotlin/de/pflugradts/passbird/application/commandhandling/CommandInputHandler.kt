package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.commandhandling.CommandType.Companion.resolveCommandTypeFrom
import de.pflugradts.passbird.application.commandhandling.command.NullCommand
import de.pflugradts.passbird.application.commandhandling.command.RepeatLastCommand
import de.pflugradts.passbird.application.commandhandling.command.base.Command
import de.pflugradts.passbird.application.commandhandling.factory.CommandFactory
import de.pflugradts.passbird.application.failure.CommandFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.domain.model.transfer.Input
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class CommandInputHandler @Inject constructor(
    private val commandBus: CommandBus,
    private val commandFactory: CommandFactory,
    private val rememberedCommandMemory: RememberedCommandMemory,
) : InputHandler {
    override fun handleInput(input: Input) {
        val originalInput = input.shell.copy()
        CommandExecutionTracker.begin()
        val command = tryCatching {
            commandFactory.construct(resolveCommandTypeFrom(input.command), input)
        }.onFailure {
            CommandExecutionTracker.markFailure()
            reportFailure(CommandFailure(it))
        }.getOrNull()
        val outcome = if (command == null) {
            CommandExecutionTracker.finish(CommandExecutionOutcome.FAILURE)
        } else {
            tryCatching {
                commandBus.post(command)
                CommandExecutionTracker.finish(command.defaultOutcome())
            }.onFailure {
                CommandExecutionTracker.markFailure()
                reportFailure(CommandFailure(it))
            }.getOrElse(CommandExecutionTracker.finish(CommandExecutionOutcome.FAILURE))
        }
        if (outcome == CommandExecutionOutcome.SUCCESS && command !is RepeatLastCommand) {
            rememberedCommandMemory.remember(originalInput)
        } else {
            originalInput.scramble()
        }
    }
}

private fun Command.defaultOutcome() = if (this is NullCommand) {
    CommandExecutionOutcome.FAILURE
} else {
    CommandExecutionOutcome.SUCCESS
}
