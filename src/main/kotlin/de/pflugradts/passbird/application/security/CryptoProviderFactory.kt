package de.pflugradts.passbird.application.security

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.boot.Bootable
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.transfer.Bytes.Companion.bytesOf
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
                userInterfaceAdapterPort.send(outputOf(bytesOf("Login failed. Shutting down.")))
                application.terminate(systemOperation)
                null
            },
        )

    private fun authenticate() = keyStoreAdapterPort.loadKey(
        receiveLogin(),
        systemOperation.getPath(configuration.adapter.keyStore.location)
            .resolve(ReadableConfiguration.KEYSTORE_FILENAME),
    )

    private fun receiveLogin() = userInterfaceAdapterPort.receiveSecurely(outputOf(bytesOf("Enter key: "))).bytes.toChars()
}
