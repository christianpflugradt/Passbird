package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler

fun createInputHandlerFor(commandBus: CommandBus) = InputHandler(commandBus, CommandFactory(NestCommandFactory()))
fun createInputHandlerFor(commandHandler: CommandHandler) =
    InputHandler(CommandBus(setOf(commandHandler)), CommandFactory(NestCommandFactory()))
