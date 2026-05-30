package de.pflugradts.passbird.application.commandhandling

import de.pflugradts.passbird.application.InactivityTerminationRequestedException
import de.pflugradts.passbird.application.StdinTerminationRequestedException
import de.pflugradts.passbird.application.commandhandling.command.base.Command
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import java.util.logging.Level
import java.util.logging.Logger

class CommandHandlerBus constructor(commandHandlers: Set<CommandHandler>) : CommandBus {
    private val commandHandlersByType = commandHandlers.groupBy { it.commandType }

    override fun post(command: Command) {
        var subscriberException: RuntimeException? = null
        commandHandlersByType[command.javaClass].orEmpty().forEach { commandHandler ->
            subscriberException = commandHandler.handleCatching(command, subscriberException)
        }
        subscriberException?.let { throw it }
    }

    private fun CommandHandler.handleCatching(command: Command, previousException: RuntimeException?) = runCatching { handle(command) }
        .fold(
            onSuccess = { previousException },
            onFailure = { exception ->
                exception.throwIfFatal()
                when (exception) {
                    is InactivityTerminationRequestedException,
                    is StdinTerminationRequestedException,
                    -> previousException ?: exception

                    else -> {
                        LOGGER.log(Level.SEVERE, "Exception thrown by command handler", exception)
                        previousException ?: exception.asRuntimeException()
                    }
                }
            },
        )

    private companion object {
        val LOGGER: Logger = Logger.getLogger(CommandHandlerBus::class.java.name)
    }
}

private fun Throwable.asRuntimeException() = this as? RuntimeException ?: RuntimeException(this)

private fun Throwable.throwIfFatal() {
    if (this is VirtualMachineError || this is ThreadDeath || this is LinkageError) throw this
}
