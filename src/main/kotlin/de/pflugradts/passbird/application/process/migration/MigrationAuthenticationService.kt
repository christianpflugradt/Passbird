package de.pflugradts.passbird.application.process.migration

import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.kotlinextensions.TryResult.Companion.success
import de.pflugradts.passbird.application.KeyStoreAdapterPort
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.PlainShell
import de.pflugradts.passbird.domain.model.shell.PlainShell.Companion.plainShellOf
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.transfer.Output.Companion.outputOf
import jakarta.inject.Inject
import jakarta.inject.Singleton

private const val DEFAULT_PROMPT = "Enter key: "

class MigrationCredentials private constructor(
    private val key: Shell,
    private val password: PlainShell,
) {
    fun keyCopy() = key.copy()
    fun passwordCopy() = plainShellOf(password.toCharArray())

    fun invalidate() {
        key.scramble()
        password.scramble()
    }

    companion object {
        fun migrationCredentialsOf(key: Shell, password: PlainShell) = MigrationCredentials(key, password)
    }
}

@Singleton
class MigrationAuthenticationService @Inject constructor(
    private val configuration: ReadableConfiguration,
    private val keyStoreAdapterPort: KeyStoreAdapterPort,
    private val userInterfaceAdapterPort: UserInterfaceAdapterPort,
    private val systemOperation: SystemOperation,
) {
    private var migrationCredentials: MigrationCredentials? = null

    fun authenticate(maxAttempts: Int = 3, prompt: String = DEFAULT_PROMPT): TryResult<MigrationCredentials> {
        migrationCredentials?.let { return success(it) }
        var result = authenticate(prompt)
        repeat(maxAttempts - 1) {
            if (result.failure) {
                result = authenticate(prompt)
            }
        }
        return result
    }

    fun invalidate() {
        migrationCredentials?.invalidate()
        migrationCredentials = null
    }

    private fun authenticate(prompt: String): TryResult<MigrationCredentials> {
        migrationCredentials?.let { return success(it) }
        val input = userInterfaceAdapterPort.receiveSecurely(outputOf(shellOf(prompt)))
        val loadPassword = input.shell.copy().toPlainShell()
        val password = input.toPlainShell()
        return keyStoreAdapterPort.loadKey(loadPassword, keyStorePath())
            .map { key ->
                MigrationCredentials.migrationCredentialsOf(key, password).also { migrationCredentials = it }
            }
            .onFailure { password.scramble() }
    }

    private fun keyStorePath() = systemOperation.resolvePath(
        configuration.adapter.keyStore.location.toDirectory(),
        ReadableConfiguration.KEYSTORE_FILENAME.toFileName(),
    )
}
