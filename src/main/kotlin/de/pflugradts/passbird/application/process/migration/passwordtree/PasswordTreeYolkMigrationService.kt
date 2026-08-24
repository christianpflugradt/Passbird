package de.pflugradts.passbird.application.process.migration.passwordtree

import de.pflugradts.kotlinextensions.MutableOption.Companion.mutableOptionOf
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.PASSWORD_TREE_FILENAME
import de.pflugradts.passbird.application.passwordtree.LegacyCurrentPasswordTreePayloadReader
import de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadWriter
import de.pflugradts.passbird.application.passwordtree.PasswordTreeSnapshot
import de.pflugradts.passbird.application.security.AesGcmCipher
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.egg.Egg
import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.egg.EggIdFavorites
import de.pflugradts.passbird.domain.model.egg.EggIdMemory
import de.pflugradts.passbird.domain.model.egg.FavoriteMap
import de.pflugradts.passbird.domain.model.egg.MemoryMap
import de.pflugradts.passbird.domain.model.egg.Protein
import de.pflugradts.passbird.domain.model.egg.Protein.Companion.createProtein
import de.pflugradts.passbird.domain.model.shell.EncryptedShell.Companion.encryptedShellOf
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.slot.Slots.Companion.slotIterator
import de.pflugradts.passbird.domain.service.password.tree.emptyFavorites
import de.pflugradts.passbird.domain.service.password.tree.emptyMemory

class PasswordTreeYolkMigrationService constructor(
    private val configuration: ReadableConfiguration,
    private val legacyCurrentPasswordTreePayloadReader: LegacyCurrentPasswordTreePayloadReader,
    private val passwordTreePayloadWriter: PasswordTreePayloadWriter,
    private val systemOperation: SystemOperation,
) {
    fun migrate(keyShell: Shell) {
        try {
            val cryptoProvider = AesGcmCipher(keyShell)
            val decryptedShell = systemOperation.readBytesFromFile(filePath)
                .let(::unwrapLegacyCurrentPasswordTree)
                .let { cryptoProvider.decrypt(encryptedShellOf(it)) }
            val snapshot = try {
                legacyCurrentPasswordTreePayloadReader.read(decryptedShell)
            } finally {
                decryptedShell.scramble()
            }
            val migratedSnapshot = snapshot.toMigratedSnapshot()
            val payloadShell = passwordTreePayloadWriter.write(migratedSnapshot)
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

    private fun PasswordTreeSnapshot.toMigratedSnapshot() = PasswordTreeSnapshot(
        eggs = eggs.map { egg -> egg.intoMigrationEggCopy() },
        favorites = favorites.toMigratedFavorites(),
        memory = memory.toMigratedMemory(),
        nests = nests.map {
            try {
                it.copy()
            } finally {
                it.scramble()
            }
        },
    )

    private fun Egg.intoMigrationEggCopy(): Egg {
        val migratedEggId = viewEggId()
        val migratedPassword = viewPassword()
        val migratedYolk = viewYolk().map { yolk ->
            val migratedSecret = yolk.viewSecret()
            try {
                mutableOptionOf(
                    de.pflugradts.passbird.domain.model.egg.Yolk.Companion.createYolk(
                        migratedSecret,
                        yolk.algorithm,
                        yolk.digits,
                        yolk.periodSeconds,
                    ),
                )
            } finally {
                migratedSecret.scramble()
            }
        }.orElse(mutableOptionOf())
        try {
            return createEgg(
                slot = associatedNest(),
                eggIdShell = migratedEggId,
                passwordShell = migratedPassword,
                proteins = proteins.map { proteinOption ->
                    if (proteinOption.isPresent) {
                        proteinOption.get().intoMigrationProteinCopy()
                    } else {
                        mutableOptionOf()
                    }
                },
                yolk = migratedYolk,
            )
        } finally {
            migratedEggId.scramble()
            migratedPassword.scramble()
        }
    }

    private fun Protein.intoMigrationProteinCopy() = run {
        val migratedType = viewType()
        val migratedStructure = viewStructure()
        try {
            mutableOptionOf(createProtein(migratedType, migratedStructure))
        } finally {
            migratedType.scramble()
            migratedStructure.scramble()
        }
    }

    private fun FavoriteMap.toMigratedFavorites() = emptyFavorites().apply {
        slotIterator().forEach { nestSlot ->
            this[nestSlot].set(
                EggIdFavorites().apply {
                    val source = this@toMigratedFavorites[nestSlot].get()
                    slotIterator().forEach { slot ->
                        source[slot].map { encryptedShell ->
                            try {
                                encryptedShell.copy()
                            } finally {
                                encryptedShell.scramble()
                            }
                        }.ifPresent { assign(slot, it) }
                    }
                },
            )
        }
    }

    private fun MemoryMap.toMigratedMemory() = emptyMemory().apply {
        slotIterator().forEach { nestSlot ->
            this[nestSlot].set(
                EggIdMemory().apply {
                    val source = this@toMigratedMemory[nestSlot].get()
                    slotIterator().forEach { slot ->
                        source[slot].map { encryptedShell ->
                            try {
                                encryptedShell.copy()
                            } finally {
                                encryptedShell.scramble()
                            }
                        }.ifPresent { this[slot].set(it) }
                    }
                },
            )
        }
    }

    private val filePath get() = systemOperation.resolvePath(
        configuration.adapter.passwordTree.location.toDirectory(),
        PASSWORD_TREE_FILENAME.toFileName(),
    )
}
