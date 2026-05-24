package de.pflugradts.passbird.application.commandhandling.factory

import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.commandhandling.CommandType
import de.pflugradts.passbird.application.commandhandling.CommandVariant
import de.pflugradts.passbird.application.commandhandling.command.ChangeMasterPasswordCommand
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
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class CommandFactory @Inject constructor(
    private val listCommandFactory: ListCommandFactory,
    private val memoryCommandFactory: MemoryCommandFactory,
    private val nestCommandFactory: NestCommandFactory,
    private val proteinCommandFactory: ProteinCommandFactory,
    private val setCommandFactory: SetCommandFactory,
) {
    fun construct(commandType: CommandType, input: Input) = constructDirectly(commandType, input)
        ?: constructWithoutArguments(commandType, input)
        ?: constructViaSpecialFactory(commandType, input)
        ?: NullCommand()

    private fun constructDirectly(commandType: CommandType, input: Input) = when (commandType) {
        CommandType.CUSTOM_SET -> CustomSetCommand(input)
        CommandType.DISCARD -> DiscardCommand(input)
        CommandType.GET -> GetCommand(input)
        CommandType.RENAME -> RenameCommand(input)
        CommandType.VIEW -> ViewCommand(input)
        else -> null
    }

    private fun constructWithoutArguments(commandType: CommandType, input: Input) = when (commandType) {
        CommandType.EXPORT -> constructWithOptionalStarVariant(input) { ExportCommand(it) }
        CommandType.HELP -> constructSafely(input) { HelpCommand() }
        CommandType.IMPORT -> constructWithOptionalStarVariant(input) { ImportCommand(it) }
        CommandType.KEYSTORE -> constructSafely(input) { ChangeMasterPasswordCommand() }
        CommandType.QUIT -> constructSafely(input) { QuitCommand(quitReason = USER) }
        else -> null
    }

    private fun constructViaSpecialFactory(commandType: CommandType, input: Input) = when (commandType) {
        CommandType.LIST -> constructSafely(listCommandFactory, input)
        CommandType.MEMORY -> constructSafely(memoryCommandFactory, input)
        CommandType.NEST -> constructSafely(nestCommandFactory, input)
        CommandType.PROTEIN -> constructSafely(proteinCommandFactory, input)
        CommandType.SET -> constructSafely(setCommandFactory, input)
        else -> null
    }

    private fun constructSafely(factory: SpecialCommandFactory, input: Input) = tryCatching { factory.constructFromInput(input) }
        .onFailure { reportFailure(CommandFailure(it)) }
        .getOrElse(NullCommand())

    private fun constructSafely(input: Input, supplier: () -> Command) = tryCatching {
        require(input.command.size == 1 && input.data.isEmpty) {
            "Parameter for command '${input.command.getChar(0)}' not supported: ${unsupportedParameter(input)}"
        }
        supplier()
    }.onFailure { reportFailure(CommandFailure(it)) }
        .getOrElse(NullCommand())

    private fun constructWithOptionalStarVariant(input: Input, supplier: (Boolean) -> Command) = tryCatching {
        require(input.data.isEmpty) {
            "Parameter for command '${input.command.getChar(0)}' not supported: ${unsupportedParameter(input)}"
        }
        require(
            input.command.size == 1 ||
                (input.command.size == 2 && input.command.getChar(1) == CommandVariant.SHOW_ALL.value),
        ) {
            "Parameter for command '${input.command.getChar(0)}' not supported: ${unsupportedParameter(input)}"
        }
        supplier(input.command.size == 2)
    }.onFailure { reportFailure(CommandFailure(it)) }
        .getOrElse(NullCommand())

    private fun unsupportedParameter(input: Input) = buildString {
        append(input.command.slice(1).asString())
        append(input.data.asString())
    }
}
