package de.pflugradts.passbird.application.process.migration.passwordtree
import de.pflugradts.kotlinextensions.MutableOption.Companion.mutableOptionOf
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.PASSWORD_TREE_FILENAME
import de.pflugradts.passbird.application.passwordtree.LegacyPasswordTreePayloadReader
import de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope
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
import de.pflugradts.passbird.domain.model.egg.Protein
import de.pflugradts.passbird.domain.model.egg.Protein.Companion.createProtein
import de.pflugradts.passbird.domain.model.shell.EncryptedShell
import de.pflugradts.passbird.domain.model.shell.EncryptedShell.Companion.encryptedShellOf
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.slot.Slots.Companion.slotIterator
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.emptyMemory
class PasswordTreeKeyDerivationMigrationService constructor(
    private val configuration: ReadableConfiguration,
    private val passwordTreeEnvelope: PasswordTreeEnvelope,
    private val legacyPasswordTreePayloadReader: LegacyPasswordTreePayloadReader,
    private val passwordTreePayloadWriter: PasswordTreePayloadWriter,
    private val systemOperation: SystemOperation,
) {
    fun migrate(keyShell: Shell) {
        try {
            val legacyProvider = createLegacyAesGcmCipher(keyShell)
            val currentProvider = AesGcmCipher(keyShell)
            val decryptedShell = systemOperation.readBytesFromFile(filePath)
                .let { legacyProvider.decrypt(encryptedShellOf(it)) }
            val snapshot = try {
                legacyPasswordTreePayloadReader.read(decryptedShell)
            } finally {
                decryptedShell.scramble()
            }
            val migratedSnapshot = snapshot.migrate(legacyProvider, currentProvider)
            val payloadShell = passwordTreePayloadWriter.write(migratedSnapshot)
            val migratedBytes = try {
                passwordTreeEnvelope.wrap(currentProvider.encrypt(payloadShell).toByteArray())
            } finally {
                payloadShell.scramble()
            }
            systemOperation.writeBytesToSensitiveFile(filePath, migratedBytes)
        } finally {
            keyShell.scramble()
        }
    }
    private fun PasswordTreeSnapshot.migrate(legacyProvider: CryptoProvider, currentProvider: CryptoProvider) = PasswordTreeSnapshot(
        eggs = eggs.map { it.migrate(legacyProvider, currentProvider) },
        memory = memory.migrate(legacyProvider, currentProvider),
        nests = nests.map {
            try {
                it.copy()
            } finally {
                it.scramble()
            }
        },
    )
    private fun Egg.migrate(legacyProvider: CryptoProvider, currentProvider: CryptoProvider): Egg {
        val migratedEggId = viewEggId().migrate(legacyProvider, currentProvider)
        val migratedPassword = viewPassword().migrate(legacyProvider, currentProvider)
        try {
            return createEgg(
                slot = associatedNest(),
                eggIdShell = migratedEggId,
                passwordShell = migratedPassword,
                proteins = proteins.map { proteinOption ->
                    if (proteinOption.isPresent) {
                        proteinOption.get().migrate(legacyProvider, currentProvider)
                    } else {
                        mutableOptionOf()
                    }
                },
            )
        } finally {
            migratedEggId.scramble()
            migratedPassword.scramble()
        }
    }
    private fun Protein.migrate(legacyProvider: CryptoProvider, currentProvider: CryptoProvider) = run {
        val migratedType = viewType().migrate(legacyProvider, currentProvider)
        val migratedStructure = viewStructure().migrate(legacyProvider, currentProvider)
        try {
            mutableOptionOf(createProtein(migratedType, migratedStructure))
        } finally {
            migratedType.scramble()
            migratedStructure.scramble()
        }
    }
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
    private fun EncryptedShell.migrate(legacyProvider: CryptoProvider, currentProvider: CryptoProvider): EncryptedShell {
        try {
            val shell = legacyProvider.decrypt(this)
            try {
                return currentProvider.encrypt(shell)
            } finally {
                shell.scramble()
            }
        } finally {
            scramble()
        }
    }
    private val filePath get() = systemOperation.resolvePath(
        configuration.adapter.passwordTree.location.toDirectory(),
        PASSWORD_TREE_FILENAME.toFileName(),
    )
}
