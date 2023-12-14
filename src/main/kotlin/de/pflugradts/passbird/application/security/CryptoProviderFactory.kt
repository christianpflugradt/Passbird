package de.pflugradts.passbird.application.security

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf

@Singleton
class CryptoProviderFactory @Inject constructor(
    @Inject private val application: Bootable,
    @Inject private val configuration: ReadableConfiguration,
    @Inject private val keyStoreAdapterPort: KeyStoreAdapterPort,
    @Inject private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    @Inject private val systemOperation: SystemOperation,
) {
    fun createCryptoProvider() = authenticate()
        .retry { authenticate() }
        .retry { authenticate() }
        .fold(
            onSuccess = { Cipherizer(it.secret, it.iv) },
            onFailure = {
                userInterfaceAdapterPort.send(outputOf(shellOf("Login failed. Shutting down.")))
                application.terminate(systemOperation)
                null
            },
        )

    private fun authenticate() = keyStoreAdapterPort.loadKey(
        receiveLogin(),
        systemOperation.resolvePath(
            configuration.adapter.keyStore.location.toDirectory(), ReadableConfiguration.KEYSTORE_FILENAME.toFileName()
        ),
    )

    private fun receiveLogin() = userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf("Enter key: "))).shell.toPlainShell()
}
