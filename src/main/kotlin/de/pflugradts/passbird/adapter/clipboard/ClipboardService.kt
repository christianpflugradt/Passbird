package de.pflugradts.passbird.adapter.clipboard

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.application.ClipboardAdapterPort
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.transfer.Output

@Singleton
class ClipboardService @Inject constructor(
    @Inject private val systemOperation: SystemOperation,
    @Inject private val configuration: ReadableConfiguration,
) : ClipboardAdapterPort {

    private var cleanerThread: Thread? = null
    override fun post(output: Output) {
        cleanerThread?.interrupt()
        systemOperation.copyToClipboard(output.bytes.asString())
        scheduleCleaner()
    }

    private fun scheduleCleaner() {
        if (isResetEnabled) {
            cleanerThread = Thread { sleep().onSuccess { systemOperation.copyToClipboard("") } }
            cleanerThread?.start()
        }
    }

    private fun sleep() = runCatching { Thread.sleep(delaySeconds * MILLI_SECONDS) }

    private val isResetEnabled: Boolean
        get() = configuration.getAdapter().getClipboard().getReset().isEnabled()
    private val delaySeconds: Int
        get() = configuration.getAdapter().getClipboard().getReset().getDelaySeconds()

    companion object {
        private const val MILLI_SECONDS = 1000L
    }
}
