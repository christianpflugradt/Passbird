package de.pflugradts.passbird.adapter.keystore

import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.PlainShell
import de.pflugradts.passbird.domain.model.shell.Shell
import jakarta.inject.Inject
import java.nio.file.Path

class JceksKeyStoreService @Inject constructor(private val systemOperation: SystemOperation) : KeyStoreAdapterPort {
    private val keyStorePersistence = KeyStorePersistence(systemOperation)

    override fun loadKey(password: PlainShell, path: Path) = keyStorePersistence.loadKey({ systemOperation.jceksInstance }, password, path)

    override fun storeKey(password: PlainShell, path: Path) =
        keyStorePersistence.storeKey({ systemOperation.jceksInstance }, password, path)

    override fun storeExistingKey(key: Shell, password: PlainShell, path: Path) =
        keyStorePersistence.storeExistingKey({ systemOperation.jceksInstance }, key, password, path)
}
