package de.pflugradts.passbird.application.process.inactivity

import com.google.inject.Inject
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.process.Initializer
import kotlin.concurrent.fixedRateTimer

class InactivityHandlerScheduler @Inject constructor(
    private val configuration: ReadableConfiguration,
    private val inactivityHandler: InactivityHandler,
) : Initializer {
    override fun run() {
        if (configuration.application.inactivityLimit.enabled) {
            fixedRateTimer(
                name = "inactivity-handler",
                daemon = true,
                initialDelay = 0,
                period = 1000 * 60,
                action = { inactivityHandler.checkInactivity() },
            )
        }
    }
}
