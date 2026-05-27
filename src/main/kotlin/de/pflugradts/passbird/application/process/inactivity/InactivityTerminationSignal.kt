package de.pflugradts.passbird.application.process.inactivity

import jakarta.inject.Singleton
import java.util.concurrent.atomic.AtomicBoolean

@Singleton
class InactivityTerminationSignal {
    private val requested = AtomicBoolean(false)

    fun request() {
        requested.set(true)
    }

    fun consume() = requested.getAndSet(false)

    fun isRequested() = requested.get()
}
