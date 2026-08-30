package de.pflugradts.passbird.application

import java.util.concurrent.atomic.AtomicReference

internal interface MacOsMainThreadExecutor {
    fun <T> dispatch(work: () -> T): T
}

internal interface MacOsMainThreadDispatcher : MacOsMainThreadExecutor {
    fun close()
}

internal object MacOsMainThreadBridge : MacOsMainThreadExecutor {
    private val dispatcher = AtomicReference<MacOsMainThreadDispatcher?>(null)

    override fun <T> dispatch(work: () -> T): T = dispatcher.get()?.dispatch(work) ?: work()

    fun install(dispatcher: MacOsMainThreadDispatcher) {
        this.dispatcher.set(dispatcher)
    }

    fun uninstall(dispatcher: MacOsMainThreadDispatcher) {
        if (this.dispatcher.compareAndSet(dispatcher, null)) {
            dispatcher.close()
        }
    }
}
