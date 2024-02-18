package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler

private val commandFactory = CommandFactory(NestCommandFactory(), ProteinCommandFactory(), SetCommandFactory())
fun createInputHandlerFor(commandBus: CommandBus) = InputHandler(commandBus, commandFactory)
fun createInputHandlerFor(commandHandler: CommandHandler) = InputHandler(CommandBus(setOf(commandHandler)), commandFactory)
