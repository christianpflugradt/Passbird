package de.pflugradts.passbird.application.process.inactivity

import com.google.inject.Inject
import de.pflugradts.passbird.application.commandhandling.CommandBus
import de.pflugradts.passbird.application.commandhandling.command.QuitCommand
import de.pflugradts.passbird.application.commandhandling.command.QuitReason.INACTIVITY
import de.pflugradts.passbird.application.configuration.ReadableConfiguration

class InactivityHandler @Inject constructor(
    @Inject private val commandBus: CommandBus,
    @Inject private val configuration: ReadableConfiguration,
) {
    private val inactivityLimitInMinutes get() = configuration.application.inactivityLimit.limitInMinutes
    private var lastInteraction = now()
    private fun now() = System.currentTimeMillis()
    fun registerInteraction() { lastInteraction = now() }
    fun checkInactivity() {
        if (((now() - lastInteraction) / 60_000) > inactivityLimitInMinutes) {
            commandBus.post(QuitCommand(quitReason = INACTIVITY))
        }
    }
}
