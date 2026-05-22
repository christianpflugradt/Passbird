package de.pflugradts.passbird.application.commandhandling

import com.google.common.eventbus.EventBus
import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.application.commandhandling.command.base.Command
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler

@Singleton
class CommandHandlerBus @Inject constructor(commandHandlers: Set<CommandHandler>) : CommandBus {
    private val eventBus = EventBus()
    init {
        commandHandlers.forEach { eventBus.register(it) }
    }
    override fun post(command: Command) = eventBus.post(command)
}
