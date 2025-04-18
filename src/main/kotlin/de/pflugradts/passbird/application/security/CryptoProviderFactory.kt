package de.pflugradts.passbird.application.security

import com.google.inject.Inject
import com.google.inject.Singleton
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.failure.LoginFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf

@Singleton
class CryptoProviderFactory @Inject constructor(
    private val configuration: ReadableConfiguration,
    private val keyStoreAdapterPort: KeyStoreAdapterPort,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val systemOperation: SystemOperation,
) {
    fun createCryptoProvider() = authenticate()
        .retry { authenticate() }
        .retry { authenticate() }
        .map { AesGcmCipher(it) }
        .onFailure {
            reportFailure(LoginFailure(3))
            systemOperation.exit()
        }
        .getOrNull()!!

    private fun authenticate() = keyStoreAdapterPort.loadKey(
        receiveLogin(),
        systemOperation.resolvePath(
            configuration.adapter.keyStore.location.toDirectory(),
            ReadableConfiguration.KEYSTORE_FILENAME.toFileName(),
        ),
    )

    private fun receiveLogin() = userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf("Enter key: "))).shell.toPlainShell()
}
