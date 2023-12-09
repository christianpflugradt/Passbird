package de.pflugradts.passbird.adapter.clipboard

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.failure.ClipboardFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.transfer.Output

private const val MILLI_SECONDS = 1000L

@Singleton
class ClipboardService @Inject constructor(
    @Inject private val systemOperation: SystemOperation,
    @Inject private val configuration: ReadableConfiguration,
) : ClipboardAdapterPort {

    private var cleanerThread: Thread? = null
    override fun post(output: Output) {
        cleanerThread?.interrupt()
        tryCatching { systemOperation.copyToClipboard(output.bytes.asString()) }
            .onFailure { reportFailure(ClipboardFailure(it)) }
        scheduleCleaner()
    }

    private fun scheduleCleaner() {
        if (isResetEnabled) {
            cleanerThread = Thread { sleep().onSuccess { tryCatching { systemOperation.copyToClipboard("") } } }
            cleanerThread!!.start()
        }
    }

    private fun sleep() = tryCatching { Thread.sleep(delaySeconds * MILLI_SECONDS) }

    private val isResetEnabled: Boolean get() = configuration.adapter.clipboard.reset.enabled
    private val delaySeconds: Int get() = configuration.adapter.clipboard.reset.delaySeconds
}
