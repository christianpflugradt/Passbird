package de.pflugradts.passbird.adapter.clipboard
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.failure.ClipboardFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.domain.model.transfer.Output
private const val MILLI_SECONDS = 1000L
class ClipboardService constructor(
    private val clipboardGateway: ClipboardGateway,
    private val configuration: ReadableConfiguration,
    private val resetScheduler: ClipboardResetScheduler = ThreadClipboardResetScheduler(),
) : ClipboardAdapterPort {
    private val cleanerLock = Any()
    private var cleanerGeneration = 0L
    override fun post(output: Output) = tryCatching {
        synchronized(cleanerLock) {
            clipboardGateway.copy(output.shell.asString(), nativeToolingEnabled)
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
            resetScheduler.schedule(delaySeconds * MILLI_SECONDS, { clearClipboard(generation) }) { error ->
                reportFailure(ClipboardFailure(error))
            }
        }
    }

    private fun clearClipboard(generation: Long) {
        synchronized(cleanerLock) {
            if (cleanerGeneration == generation) {
                tryCatching { clipboardGateway.copy("", nativeToolingEnabled) }.onFailure { reportFailure(ClipboardFailure(it)) }
            }
        }
    }

    private val nativeToolingEnabled: Boolean get() = configuration.adapter.clipboard.nativeTooling.enabled
    private val isResetEnabled: Boolean get() = configuration.adapter.clipboard.reset.enabled
    private val delaySeconds: Int get() = configuration.adapter.clipboard.reset.delaySeconds
}

fun interface ClipboardResetScheduler {
    fun schedule(delayMillis: Long, task: () -> Unit, onFailure: (Exception) -> Unit)
}

class ThreadClipboardResetScheduler : ClipboardResetScheduler {
    override fun schedule(delayMillis: Long, task: () -> Unit, onFailure: (Exception) -> Unit) {
        Thread {
            try {
                Thread.sleep(delayMillis)
                task()
            } catch (error: InterruptedException) {
                onFailure(error)
            } catch (error: IllegalArgumentException) {
                onFailure(error)
            }
        }.start()
    }
}
