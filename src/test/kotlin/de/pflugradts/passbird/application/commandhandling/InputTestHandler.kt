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

fun createInputHandlerFor(commandBus: CommandBus): InputHandler = CommandInputHandler(commandBus, commandFactory)
fun createInputHandlerFor(commandHandler: CommandHandler): InputHandler =
    CommandInputHandler(CommandHandlerBus(setOf(commandHandler)), commandFactory)
