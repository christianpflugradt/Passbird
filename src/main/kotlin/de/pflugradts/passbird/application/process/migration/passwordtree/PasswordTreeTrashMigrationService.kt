package de.pflugradts.passbird.application.process.migration.passwordtree

import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.PASSWORD_TREE_FILENAME
import de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadReader
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadWriter
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.EncryptedShell.Companion.encryptedShellOf
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider

class PasswordTreeTrashMigrationService constructor(
    private val configuration: ReadableConfiguration,
    private val passwordTreePayloadReader: PasswordTreePayloadReader,
    private val passwordTreePayloadWriter: PasswordTreePayloadWriter,
    private val systemOperation: SystemOperation,
) {
    fun migrate(keyShell: Shell) {
        try {
            val cryptoProvider: CryptoProvider = de.pflugradts.passbird.application.security.AesGcmCipher(keyShell)
            val decryptedShell = systemOperation.readBytesFromFile(filePath)
                .let(::unwrapLegacyTrashPasswordTree)
                .let { cryptoProvider.decrypt(encryptedShellOf(it)) }
            val snapshot = try {
                passwordTreePayloadReader.read(decryptedShell)
            } finally {
                decryptedShell.scramble()
            }
            val payloadShell = passwordTreePayloadWriter.write(snapshot)
            val migratedBytes = try {
                PasswordTreeEnvelope().wrap(cryptoProvider.encrypt(payloadShell).toByteArray())
            } finally {
                payloadShell.scramble()
            }
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
