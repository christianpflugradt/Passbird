package de.pflugradts.passbird.application.commandhandling.handler

import de.pflugradts.passbird.application.commandhandling.command.base.Command

interface CommandHandler {
    val commandType: Class<out Command>

    fun handle(command: Command)
}

abstract class TypedCommandHandler<C : Command>(
    private val handledCommandType: Class<C>,
) : CommandHandler {
    final override val commandType: Class<out Command> = handledCommandType

    final override fun handle(command: Command) {
        handleCommand(handledCommandType.cast(command))
    }

    protected abstract fun handleCommand(command: C)
}
