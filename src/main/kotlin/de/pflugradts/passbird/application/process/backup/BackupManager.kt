package de.pflugradts.passbird.application.process.backup

import com.google.inject.Inject
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.process.Finalizer
import de.pflugradts.passbird.application.util.SystemOperation

class BackupManager @Inject constructor(
    @Inject private val configuration: ReadableConfiguration,
    @Inject private val systemOperation: SystemOperation,
) : Finalizer {
    override fun run() {
    }
}
