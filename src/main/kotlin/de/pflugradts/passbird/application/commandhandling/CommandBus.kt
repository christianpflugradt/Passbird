package de.pflugradts.passbird.application.commandhandling

import com.google.common.eventbus.EventBus
import com.google.inject.Inject
import de.pflugradts.passbird.application.commandhandling.command.Command
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler

class CommandBus @Inject constructor(
    @Inject private val commandHandlers: Set<CommandHandler>,
) {
    private val eventBus = EventBus()
    init { commandHandlers.forEach { eventBus.register(it) } }
    fun post(command: Command) = eventBus.post(command)
}
