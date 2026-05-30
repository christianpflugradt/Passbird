package de.pflugradts.passbird.application.commandhandling
import com.google.common.eventbus.EventBus
import de.pflugradts.passbird.application.InactivityTerminationRequestedException
import de.pflugradts.passbird.application.StdinTerminationRequestedException
import de.pflugradts.passbird.application.commandhandling.command.base.Command
import de.pflugradts.passbird.application.commandhandling.handler.CommandHandler
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Level
import java.util.logging.Logger
class CommandHandlerBus constructor(commandHandlers: Set<CommandHandler>) : CommandBus {
    private val subscriberException = AtomicReference<RuntimeException?>()
    private val eventBus = EventBus { exception, _ ->
        when (exception) {
            is InactivityTerminationRequestedException,
            is StdinTerminationRequestedException,
            -> subscriberException.compareAndSet(null, exception)

            else -> {
                LOGGER.log(Level.SEVERE, "Exception thrown by command handler", exception)
                subscriberException.compareAndSet(null, exception.asRuntimeException())
            }
        }
    }
    init {
        commandHandlers.forEach { eventBus.register(it) }
    }
    override fun post(command: Command) {
        subscriberException.set(null)
        eventBus.post(command)
        subscriberException.getAndSet(null)?.let { throw it }
    }
    private companion object {
        val LOGGER: Logger = Logger.getLogger(CommandHandlerBus::class.java.name)
    }
}
private fun Throwable.asRuntimeException() = this as? RuntimeException ?: RuntimeException(this)
