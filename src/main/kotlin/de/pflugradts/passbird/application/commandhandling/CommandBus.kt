package de.pflugradts.passbird.application.commandhandling

import com.google.common.eventbus.EventBus
import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.application.commandhandling.command.base.Command
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler

@Singleton
class CommandBus @Inject constructor(commandHandlers: Set<CommandHandler>) {
    private val eventBus = EventBus()
    init {
        commandHandlers.forEach { eventBus.register(it) }
    }
    fun post(command: Command) = eventBus.post(command)
}
