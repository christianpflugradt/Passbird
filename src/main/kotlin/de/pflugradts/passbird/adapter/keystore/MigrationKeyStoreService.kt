package de.pflugradts.passbird.adapter.keystore

import de.pflugradts.kotlinextensions.TryResult.Companion.failure
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.keystore.KeyStoreFormat
import de.pflugradts.passbird.application.keystore.KeyStoreFormatDetector
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.PlainShell
import de.pflugradts.passbird.domain.model.shell.Shell
import jakarta.inject.Inject
import java.nio.file.Path

class MigrationKeyStoreService @Inject constructor(
    private val currentKeyStoreService: KeyStoreService,
    private val jceksKeyStoreService: JceksKeyStoreService,
    private val keyStoreFormatDetector: KeyStoreFormatDetector,
    private val systemOperation: SystemOperation,
) : KeyStoreAdapterPort {

    override fun loadKey(password: PlainShell, path: Path) = tryCatching {
        keyStoreFormatDetector.detect(systemOperation.readBytesFromFile(path))
    }.let { format ->
        if (format.failure) {
            password.scramble()
            failure(format.exceptionOrNull()!!)
        } else {
            when (format.getOrNull()!!) {
                KeyStoreFormat.JCEKS -> jceksKeyStoreService.loadKey(password, path)
                KeyStoreFormat.PKCS12, KeyStoreFormat.UNKNOWN -> currentKeyStoreService.loadKey(password, path)
            }
        }
    }

    override fun storeKey(password: PlainShell, path: Path) = currentKeyStoreService.storeKey(password, path)

    override fun storeExistingKey(key: Shell, password: PlainShell, path: Path) =
        currentKeyStoreService.storeExistingKey(key, password, path)
}
