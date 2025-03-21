package de.pflugradts.passbird.application.commandhandling.factory

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.commandhandling.CommandType
import de.pflugradts.passbird.application.commandhandling.command.CustomSetCommand
import de.pflugradts.passbird.application.commandhandling.command.DiscardCommand
import de.pflugradts.passbird.application.commandhandling.command.ExportCommand
import de.pflugradts.passbird.application.commandhandling.command.GetCommand
import de.pflugradts.passbird.application.commandhandling.command.HelpCommand
import de.pflugradts.passbird.application.commandhandling.command.ImportCommand
import de.pflugradts.passbird.application.commandhandling.command.ListCommand
import de.pflugradts.passbird.application.commandhandling.command.NullCommand
import de.pflugradts.passbird.application.commandhandling.command.QuitCommand
import de.pflugradts.passbird.application.commandhandling.command.QuitReason.USER
import de.pflugradts.passbird.application.commandhandling.command.RenameCommand
import de.pflugradts.passbird.application.commandhandling.command.ViewCommand
import de.pflugradts.passbird.application.failure.CommandFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.domain.model.transfer.Input

@Singleton
class CommandFactory @Inject constructor(
    private val memoryCommandFactory: MemoryCommandFactory,
    private val nestCommandFactory: NestCommandFactory,
    private val proteinCommandFactory: ProteinCommandFactory,
    private val setCommandFactory: SetCommandFactory,
) {
    fun construct(commandType: CommandType, input: Input) = when (commandType) {
        CommandType.CUSTOM_SET -> CustomSetCommand(input)
        CommandType.DISCARD -> DiscardCommand(input)
        CommandType.EXPORT -> ExportCommand()
        CommandType.GET -> GetCommand(input)
        CommandType.HELP -> HelpCommand()
        CommandType.IMPORT -> ImportCommand()
        CommandType.LIST -> ListCommand()
        CommandType.MEMORY -> constructSafely(memoryCommandFactory, input)
        CommandType.NEST -> constructSafely(nestCommandFactory, input)
        CommandType.PROTEIN -> constructSafely(proteinCommandFactory, input)
        CommandType.QUIT -> QuitCommand(quitReason = USER)
        CommandType.RENAME -> RenameCommand(input)
        CommandType.SET -> constructSafely(setCommandFactory, input)
        CommandType.VIEW -> ViewCommand(input)
        else -> NullCommand()
    }

    private fun constructSafely(factory: SpecialCommandFactory, input: Input) = tryCatching { factory.constructFromInput(input) }
        .onFailure { reportFailure(CommandFailure(it)) }
        .getOrElse(NullCommand())
}
