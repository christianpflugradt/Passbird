package de.pflugradts.passbird.application.commandhandling

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.application.commandhandling.command.CustomSetCommand
import de.pflugradts.passbird.application.commandhandling.command.DiscardCommand
import de.pflugradts.passbird.application.commandhandling.command.ExportCommand
import de.pflugradts.passbird.application.commandhandling.command.GetCommand
import de.pflugradts.passbird.application.commandhandling.command.HelpCommand
import de.pflugradts.passbird.application.commandhandling.command.ImportCommand
import de.pflugradts.passbird.application.commandhandling.command.ListCommand
import de.pflugradts.passbird.application.commandhandling.command.NullCommand
import de.pflugradts.passbird.application.commandhandling.command.QuitCommand
import de.pflugradts.passbird.application.commandhandling.command.RenameCommand
import de.pflugradts.passbird.application.commandhandling.command.SetCommand
import de.pflugradts.passbird.application.commandhandling.command.ViewCommand
import de.pflugradts.passbird.application.commandhandling.command.base.Command
import de.pflugradts.passbird.domain.model.transfer.Input

@Singleton
class CommandFactory @Inject constructor(
    @Inject private val namespaceCommandFactory: NamespaceCommandFactory,
) {
    fun construct(commandType: CommandType, input: Input): Command {
        return when (commandType) {
            CommandType.CUSTOM_SET -> CustomSetCommand(input)
            CommandType.DISCARD -> DiscardCommand(input)
            CommandType.EXPORT -> ExportCommand(input)
            CommandType.GET -> GetCommand(input)
            CommandType.HELP -> HelpCommand()
            CommandType.IMPORT -> ImportCommand(input)
            CommandType.LIST -> ListCommand()
            CommandType.NAMESPACE -> namespaceCommandFactory.constructFromInput(input)
            CommandType.QUIT -> QuitCommand()
            CommandType.RENAME -> RenameCommand(input)
            CommandType.SET -> SetCommand(input)
            CommandType.VIEW -> ViewCommand(input)
            else -> NullCommand()
        }
    }
}