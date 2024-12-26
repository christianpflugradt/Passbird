package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.commandhandling.factory.CommandFactory
import de.pflugradts.passbird.application.commandhandling.factory.MemoryCommandFactory
import de.pflugradts.passbird.application.commandhandling.factory.NestCommandFactory
import de.pflugradts.passbird.application.commandhandling.factory.ProteinCommandFactory
import de.pflugradts.passbird.application.commandhandling.factory.SetCommandFactory
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler

private val commandFactory = CommandFactory(
    memoryCommandFactory = MemoryCommandFactory(),
    nestCommandFactory = NestCommandFactory(),
    proteinCommandFactory = ProteinCommandFactory(),
    setCommandFactory = SetCommandFactory(),
)

fun createInputHandlerFor(commandBus: CommandBus) = InputHandler(commandBus, commandFactory)
fun createInputHandlerFor(commandHandler: CommandHandler) = InputHandler(CommandBus(setOf(commandHandler)), commandFactory)
