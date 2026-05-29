package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.commandhandling.factory.CommandFactory
import de.pflugradts.passbird.application.commandhandling.factory.FavoriteCommandFactory
import de.pflugradts.passbird.application.commandhandling.factory.ListCommandFactory
import de.pflugradts.passbird.application.commandhandling.factory.MemoryCommandFactory
import de.pflugradts.passbird.application.commandhandling.factory.NestCommandFactory
import de.pflugradts.passbird.application.commandhandling.factory.ProteinCommandFactory
import de.pflugradts.passbird.application.commandhandling.factory.SetCommandFactory
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler

private val commandFactory = CommandFactory(
    favoriteCommandFactory = FavoriteCommandFactory(),
    listCommandFactory = ListCommandFactory(),
    memoryCommandFactory = MemoryCommandFactory(),
    nestCommandFactory = NestCommandFactory(),
    proteinCommandFactory = ProteinCommandFactory(),
    setCommandFactory = SetCommandFactory(),
)

fun createInputHandlerFor(
    commandBus: CommandBus,
    rememberedCommandMemory: RememberedCommandMemory = RememberedCommandMemory(),
    commandExecutionTracker: CommandExecutionTracker = CommandExecutionTracker(),
): InputHandler = CommandInputHandler(commandBus, commandFactory, rememberedCommandMemory, commandExecutionTracker)
fun createInputHandlerFor(
    commandHandler: CommandHandler,
    commandExecutionTracker: CommandExecutionTracker = CommandExecutionTracker(),
): InputHandler = CommandInputHandler(
    CommandHandlerBus(setOf(commandHandler)),
    commandFactory,
    RememberedCommandMemory(),
    commandExecutionTracker,
)
