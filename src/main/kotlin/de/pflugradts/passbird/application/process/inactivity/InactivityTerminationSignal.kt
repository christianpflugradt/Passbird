package de.pflugradts.passbird.application.process.inactivity
import java.util.concurrent.atomic.AtomicBoolean
class InactivityTerminationSignal {
    private val requested = AtomicBoolean(false)
    fun request() {
        requested.set(true)
    }
    fun consume() = requested.getAndSet(false)
    fun isRequested() = requested.get()
}
