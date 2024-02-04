package de.pflugradts.passbird.application.commandhandling

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.kotlinextensions.tryCatching
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
import de.pflugradts.passbird.application.commandhandling.command.base.Command
import de.pflugradts.passbird.application.failure.CommandFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.domain.model.transfer.Input

@Singleton
class CommandFactory @Inject constructor(
    @Inject private val nestCommandFactory: NestCommandFactory,
    @Inject private val setCommandFactory: SetCommandFactory,
) {
    fun construct(commandType: CommandType, input: Input): Command {
        return when (commandType) {
            CommandType.CUSTOM_SET -> CustomSetCommand(input)
            CommandType.DISCARD -> DiscardCommand(input)
            CommandType.EXPORT -> ExportCommand()
            CommandType.GET -> GetCommand(input)
            CommandType.HELP -> HelpCommand()
            CommandType.IMPORT -> ImportCommand()
            CommandType.LIST -> ListCommand()
            CommandType.NEST ->
                tryCatching { nestCommandFactory.constructFromInput(input) }
                    .onFailure { reportFailure(CommandFailure(it)) }
                    .getOrElse(NullCommand())
            CommandType.QUIT -> QuitCommand(quitReason = USER)
            CommandType.RENAME -> RenameCommand(input)
            CommandType.SET ->
                tryCatching { setCommandFactory.constructFromInput(input) }
                    .onFailure { reportFailure(CommandFailure(it)) }
                    .getOrElse(NullCommand())
            CommandType.VIEW -> ViewCommand(input)
            else -> NullCommand()
        }
    }
}
