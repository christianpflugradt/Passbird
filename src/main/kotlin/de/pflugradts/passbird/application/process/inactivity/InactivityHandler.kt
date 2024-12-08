package de.pflugradts.passbird.application.process.inactivity

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.application.commandhandling.CommandBus
import de.pflugradts.passbird.application.commandhandling.command.QuitCommand
import de.pflugradts.passbird.application.commandhandling.command.QuitReason.INACTIVITY
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import java.util.concurrent.atomic.AtomicLong

@Singleton
class InactivityHandler @Inject constructor(
    private val commandBus: CommandBus,
    private val configuration: ReadableConfiguration,
    private val systemOperation: SystemOperation,
) {
    private val inactivityLimitInMinutes get() = configuration.application.inactivityLimit.limitInMinutes
    private val lastInteraction = AtomicLong(now())

    private fun now() = systemOperation.clock.instant().epochSecond

    fun registerInteraction() = lastInteraction.set(now())

    fun checkInactivity() = run {
        if ((now() - lastInteraction.get()) > (inactivityLimitInMinutes * 60)) {
            commandBus.post(QuitCommand(quitReason = INACTIVITY))
        }
    }
}
