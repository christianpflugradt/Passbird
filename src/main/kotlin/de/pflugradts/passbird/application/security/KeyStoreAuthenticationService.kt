package de.pflugradts.passbird.application.security

import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.kotlinextensions.TryResult.Companion.failure
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.SecureInputUnavailableException
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class KeyStoreAuthenticationService @Inject constructor(
    private val configuration: ReadableConfiguration,
    private val keyStoreAdapterPort: KeyStoreAdapterPort,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val systemOperation: SystemOperation,
) {
    fun authenticate(maxAttempts: Int = 3, prompt: String = "Enter key: "): TryResult<Shell> {
        var result = authenticate(prompt)
        repeat(maxAttempts - 1) {
            if (result.failure) {
                result = authenticate(prompt)
            }
        }
        return result
    }

    fun keyStorePath() = systemOperation.resolvePath(
        configuration.adapter.keyStore.location.toDirectory(),
        ReadableConfiguration.KEYSTORE_FILENAME.toFileName(),
    )

    private fun authenticate(prompt: String): TryResult<Shell> = try {
        keyStoreAdapterPort.loadKey(
            userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf(prompt))).toPlainShell(),
            keyStorePath(),
        )
    } catch (ex: SecureInputUnavailableException) {
        failure(ex)
    }
}
