package de.pflugradts.passbird.adapter.clipboard

import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.failure.ClipboardFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.transfer.Output
import jakarta.inject.Inject
import jakarta.inject.Singleton

private const val MILLI_SECONDS = 1000L

@Singleton
class ClipboardService @Inject constructor(
    private val systemOperation: SystemOperation,
    private val configuration: ReadableConfiguration,
) : ClipboardAdapterPort {
    private val cleanerLock = Any()
    private var cleanerGeneration = 0L

    override fun post(output: Output) = tryCatching {
        synchronized(cleanerLock) {
            systemOperation.copyToClipboard(output.shell.asString())
            cleanerGeneration += 1
            cleanerGeneration
        }
    }.onFailure {
        reportFailure(ClipboardFailure(it))
    }.onSuccess(::scheduleCleaner).map {
        Unit
    }

    private fun scheduleCleaner(generation: Long) {
        if (isResetEnabled) {
            Thread {
                sleep().onSuccess {
                    synchronized(cleanerLock) {
                        if (cleanerGeneration == generation) {
                            tryCatching { systemOperation.copyToClipboard("") }
                        }
                    }
                }
            }.start()
        }
    }

    private fun sleep() = tryCatching { Thread.sleep(delaySeconds * MILLI_SECONDS) }

    private val isResetEnabled: Boolean get() = configuration.adapter.clipboard.reset.enabled
    private val delaySeconds: Int get() = configuration.adapter.clipboard.reset.delaySeconds
}
