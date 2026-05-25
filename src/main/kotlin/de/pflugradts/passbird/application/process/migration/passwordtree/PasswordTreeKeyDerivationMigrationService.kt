package de.pflugradts.passbird.application.process.migration.passwordtree

import de.pflugradts.kotlinextensions.MutableOption.Companion.mutableOptionOf
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.PASSWORD_TREE_FILENAME
import de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadReader
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadWriter
import de.pflugradts.passbird.application.passwordtree.PasswordTreeSnapshot
import de.pflugradts.passbird.application.security.AesGcmCipher
import de.pflugradts.passbird.application.security.createLegacyAesGcmCipher
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.egg.EggIdMemory
import de.pflugradts.passbird.domain.model.egg.MemoryMap
import de.pflugradts.passbird.domain.model.egg.Protein.Companion.createProtein
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.shell.EncryptedShell.Companion.encryptedShellOf
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.slot.Slots.Companion.slotIterator
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.emptyMemory
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class PasswordTreeKeyDerivationMigrationService @Inject constructor(
    private val configuration: ReadableConfiguration,
    private val passwordTreeEnvelope: PasswordTreeEnvelope,
    private val passwordTreePayloadReader: PasswordTreePayloadReader,
    private val passwordTreePayloadWriter: PasswordTreePayloadWriter,
    private val systemOperation: SystemOperation,
) {
    fun migrate(keyShell: Shell) {
        val legacyProvider = createLegacyAesGcmCipher(keyShell)
        val currentProvider = AesGcmCipher(keyShell)
        val snapshot = systemOperation.readBytesFromFile(filePath)
            .let { legacyProvider.decrypt(encryptedShellOf(it)) }
            .let(passwordTreePayloadReader::read)
        val migratedSnapshot = snapshot.migrate(legacyProvider, currentProvider)
        val migratedBytes = passwordTreeEnvelope.wrap(
            currentProvider.encrypt(passwordTreePayloadWriter.write(migratedSnapshot)).toByteArray(),
        )
        systemOperation.writeBytesToSensitiveFile(filePath, migratedBytes)
    }

    private fun PasswordTreeSnapshot.migrate(legacyProvider: CryptoProvider, currentProvider: CryptoProvider) = PasswordTreeSnapshot(
        eggs = eggs.map { it.migrate(legacyProvider, currentProvider) },
        memory = memory.migrate(legacyProvider, currentProvider),
        nests = nests.map(Shell::copy),
    )

    private fun Egg.migrate(legacyProvider: CryptoProvider, currentProvider: CryptoProvider) = createEgg(
        slot = associatedNest(),
        eggIdShell = viewEggId().migrate(legacyProvider, currentProvider),
        passwordShell = viewPassword().migrate(legacyProvider, currentProvider),
        proteins = proteins.map { proteinOption ->
            if (proteinOption.isPresent) {
                mutableOptionOf(
                    createProtein(
                        proteinOption.get().viewType().migrate(legacyProvider, currentProvider),
                        proteinOption.get().viewStructure().migrate(legacyProvider, currentProvider),
                    ),
                )
            } else {
                mutableOptionOf()
            }
        },
    )

    private fun MemoryMap.migrate(legacyProvider: CryptoProvider, currentProvider: CryptoProvider) = emptyMemory().apply {
        slotIterator().forEach { nestSlot ->
            this[nestSlot].set(
                EggIdMemory().apply {
                    val source = this@migrate[nestSlot].get()
                    slotIterator().forEach { slot ->
                        source[slot].map { it.migrate(legacyProvider, currentProvider) }.ifPresent { this[slot].set(it) }
                    }
                },
            )
        }
    }

    private fun EncryptedShell.migrate(legacyProvider: CryptoProvider, currentProvider: CryptoProvider) =
        currentProvider.encrypt(legacyProvider.decrypt(this))

    private val filePath get() = systemOperation.resolvePath(
        configuration.adapter.passwordTree.location.toDirectory(),
        PASSWORD_TREE_FILENAME.toFileName(),
    )
}
