package de.pflugradts.passbird.application.process.migration.passwordtree

import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.PASSWORD_TREE_FILENAME
import de.pflugradts.passbird.application.passwordtree.LegacyPasswordTreePayloadReader
import de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadWriter
import de.pflugradts.passbird.application.security.AesGcmCipher
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.EncryptedShell.Companion.encryptedShellOf
import de.pflugradts.passbird.domain.model.shell.Shell
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class PasswordTreeFavoritesMigrationService @Inject constructor(
    private val configuration: ReadableConfiguration,
    private val legacyPasswordTreePayloadReader: LegacyPasswordTreePayloadReader,
    private val passwordTreeEnvelope: PasswordTreeEnvelope,
    private val passwordTreePayloadWriter: PasswordTreePayloadWriter,
    private val systemOperation: SystemOperation,
) {
    fun migrate(keyShell: Shell) {
        try {
            val cryptoProvider = AesGcmCipher(keyShell)
            val snapshot = systemOperation.readBytesFromFile(filePath)
                .let(passwordTreeEnvelope::unwrapLegacyCurrent)
                .let { cryptoProvider.decrypt(encryptedShellOf(it)) }
                .let(legacyPasswordTreePayloadReader::read)
            val migratedBytes = passwordTreeEnvelope.wrap(
                cryptoProvider.encrypt(passwordTreePayloadWriter.write(snapshot)).toByteArray(),
            )
            systemOperation.writeBytesToSensitiveFile(filePath, migratedBytes)
        } finally {
            keyShell.scramble()
        }
    }

    private val filePath get() = systemOperation.resolvePath(
        configuration.adapter.passwordTree.location.toDirectory(),
        PASSWORD_TREE_FILENAME.toFileName(),
    )
}
