package de.pflugradts.passbird.adapter.passwordtree

import de.pflugradts.kotlinextensions.tryCatching
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.PASSWORD_TREE_FILENAME
import de.pflugradts.passbird.application.failure.DecryptPasswordTreeFailure
import de.pflugradts.passbird.application.failure.reportFailure
import de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadReader
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.EncryptedShell.Companion.encryptedShellOf
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.service.nest.NestService
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.EggStreamSupplier
import jakarta.inject.Inject

class PasswordTreeReader @Inject constructor(
    private val systemOperation: SystemOperation,
    private val configuration: ReadableConfiguration,
    private val nestService: NestService,
    private val cryptoProvider: CryptoProvider,
    private val passwordTreeEnvelope: PasswordTreeEnvelope,
    private val passwordTreePayloadReader: PasswordTreePayloadReader,
) {
    fun restore(): EggStreamSupplier {
        val snapshot = passwordTreePayloadReader.read(readFromDisk())
        nestService.populate(snapshot.nests)
        return EggStreamSupplier({ snapshot.eggs.stream() }, snapshot.memory, snapshot.favorites)
    }

    private fun readFromDisk() = tryCatching {
        if (!systemOperation.exists(filePath)) {
            return@tryCatching emptyShell()
        }
        systemOperation.readBytesFromFile(filePath).let {
            if (it.isEmpty()) emptyShell() else cryptoProvider.decrypt(encryptedShellOf(passwordTreeEnvelope.unwrap(it)))
        }
    }
        .onFailure(::abortRestore)
        .getOrNull()!!

    private fun abortRestore(ex: Exception): Nothing {
        reportFailure(DecryptPasswordTreeFailure(filePath, ex))
        systemOperation.exit()
        throw ex
    }

    private val filePath get() =
        systemOperation.resolvePath(configuration.adapter.passwordTree.location.toDirectory(), PASSWORD_TREE_FILENAME.toFileName())
}
