package de.pflugradts.passbird.adapter.keystore
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.PlainShell
import de.pflugradts.passbird.domain.model.shell.Shell
import java.nio.file.Path
class JceksKeyStoreService constructor(
    private val systemOperation: SystemOperation,
    private val keyStoreFactory: KeyStoreFactory,
) : KeyStoreAdapterPort {
    private val keyStorePersistence = KeyStorePersistence(systemOperation)
    override fun loadKey(password: PlainShell, path: Path) = keyStorePersistence.loadKey(
        { keyStoreFactory.jceksInstance },
        password,
        path,
    )
    override fun storeKey(password: PlainShell, path: Path) =
        keyStorePersistence.storeKey({ keyStoreFactory.jceksInstance }, password, path)
    override fun storeExistingKey(key: Shell, password: PlainShell, path: Path) =
        keyStorePersistence.storeExistingKey({ keyStoreFactory.jceksInstance }, key, password, path)
}
