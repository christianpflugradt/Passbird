package de.pflugradts.passbird.adapter.passwordtree

import de.pflugradts.kotlinextensions.TryResult
import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.PASSWORD_TREE_FILENAME
import de.pflugradts.passbird.application.failure.WritePasswordTreeFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadWriter
import de.pflugradts.passbird.application.passwordtree.PasswordTreeSnapshot
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.EggStreamSupplier
import jakarta.inject.Inject

class PasswordTreeWriter @Inject constructor(
    private val systemOperation: SystemOperation,
    private val configuration: ReadableConfiguration,
    private val cryptoProvider: CryptoProvider,
    private val passwordTreeEnvelope: PasswordTreeEnvelope,
    private val passwordTreePayloadWriter: PasswordTreePayloadWriter,
) {

    fun sync(eggSupplier: EggStreamSupplier): TryResult<Unit> {
        val eggs = eggSupplier.get().toList()
        return writeToDisk(
            passwordTreePayloadWriter.write(
                PasswordTreeSnapshot(
                    eggs = eggs,
                    favorites = eggSupplier.favorites(),
                    memory = eggSupplier.memory(),
                    nests = eggSupplier.nests(),
                ),
            ),
        )
    }

    private fun writeToDisk(shell: Shell) = tryCatching {
        systemOperation.writeBytesToSensitiveFile(filePath, passwordTreeEnvelope.wrap(cryptoProvider.encrypt(shell).toByteArray()))
        Unit
    }.onFailure { reportFailure(WritePasswordTreeFailure(filePath, it)) }

    private val filePath get() =
        systemOperation.resolvePath(configuration.adapter.passwordTree.location.toDirectory(), PASSWORD_TREE_FILENAME.toFileName())
}
