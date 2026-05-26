package de.pflugradts.passbird.application.process.migration.passwordtree

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.adapter.passwordtree.PasswordTreeReader
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.passwordtree.LegacyPasswordTreePayloadReader
import de.pflugradts.passbird.application.passwordtree.LegacyPasswordTreePayloadWriter
import de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadReader
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadWriter
import de.pflugradts.passbird.application.passwordtree.PasswordTreeSnapshot
import de.pflugradts.passbird.application.security.createAesGcmCipherForTesting
import de.pflugradts.passbird.application.security.createLegacyAesGcmCipherForTesting
import de.pflugradts.passbird.application.security.createTestKeyShell
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.password.tree.emptyMemory
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

@Tag(INTEGRATION)
class PasswordTreeKeyDerivationMigrationServiceTest {
    private val configuration = mockk<Configuration>()
    private val systemOperation = SystemOperation()
    private val passwordTreeEnvelope = PasswordTreeEnvelope()
    private val legacyPasswordTreePayloadWriter = LegacyPasswordTreePayloadWriter()
    private val passwordTreePayloadWriter = PasswordTreePayloadWriter()
    private lateinit var passwordTreeDirectory: Path
    private lateinit var passwordTreeFile: Path

    @BeforeEach
    fun setup() {
        passwordTreeDirectory = Files.createTempDirectory("passbird-key-derivation-migration")
        passwordTreeFile = passwordTreeDirectory.resolve(ReadableConfiguration.PASSWORD_TREE_FILENAME)
        fakeConfiguration(
            instance = configuration,
            withPasswordTreeLocation = passwordTreeDirectory.toString(),
            withEggIdMemoryEnabled = true,
            withEggIdMemoryPersisted = true,
        )
    }

    @AfterEach
    fun cleanup() {
        expectThat(File(passwordTreeDirectory.toString()).deleteRecursively()).isTrue()
    }

    @Test
    fun `should migrate legacy password tree content to current format without losing secrets`() {
        val legacyCryptoProvider = createLegacyAesGcmCipherForTesting()
        val currentCryptoProvider = createAesGcmCipherForTesting()
        val detector = PasswordTreeKeyDerivationMigrationDetector(configuration, passwordTreeEnvelope, systemOperation)
        writeLegacyPasswordTree(createLegacySnapshot(legacyCryptoProvider), legacyCryptoProvider)
        expectThat(detector.detect().required).isTrue()
        createMigrationService().migrate(createTestKeyShell())
        val restored = createPasswordTreeReader(currentCryptoProvider).restore()

        expectThat(detector.detect().required) isEqualTo false
        expectThat(passwordTreeEnvelope.isCurrent(Files.readAllBytes(passwordTreeFile))).isTrue()
        expectThat(restored.get().toList().map { it.decryptedSummary(currentCryptoProvider) }) containsExactly listOf(
            "DEFAULT:email:Password1:user:user@example.com",
            "S3:bank:Password2",
        )
        expectThat(
            restored.memory()[Slot.DEFAULT].get()[0].map { currentCryptoProvider.decrypt(it).asString() }.orElse(""),
        ) isEqualTo "email"
        expectThat(
            restored.memory()[Slot.S3].get()[0].map { currentCryptoProvider.decrypt(it).asString() }.orElse(""),
        ) isEqualTo "bank"
        expectThat(restored.favorites()[Slot.DEFAULT].get().any { it.isPresent }).isEqualTo(false)
        expectThat(restored.nests()[Slot.S1.index() - 1].asString()) isEqualTo "work"
        expectThat(restored.nests()[Slot.S3.index() - 1].asString()) isEqualTo "finance"
    }

    private fun createLegacySnapshot(
        legacyCryptoProvider: de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider,
    ): PasswordTreeSnapshot {
        val defaultEgg = createEgg(
            slot = Slot.DEFAULT,
            eggIdShell = legacyCryptoProvider.encrypt(shellOf("email")),
            passwordShell = legacyCryptoProvider.encrypt(shellOf("Password1")),
        ).apply {
            updateProtein(
                slot = Slot.S1,
                typeShell = legacyCryptoProvider.encrypt(shellOf("user")),
                structureShell = legacyCryptoProvider.encrypt(shellOf("user@example.com")),
            )
        }
        val nestedEgg = createEgg(
            slot = Slot.S3,
            eggIdShell = legacyCryptoProvider.encrypt(shellOf("bank")),
            passwordShell = legacyCryptoProvider.encrypt(shellOf("Password2")),
        )
        return PasswordTreeSnapshot(
            eggs = listOf(defaultEgg, nestedEgg),
            memory = emptyMemory().apply {
                this[Slot.DEFAULT].get().memorize(defaultEgg.viewEggId(), null)
                this[Slot.S3].get().memorize(nestedEgg.viewEggId(), null)
            },
            nests = listOf(
                shellOf("work"),
                emptyShell(),
                shellOf("finance"),
                emptyShell(),
                emptyShell(),
                emptyShell(),
                emptyShell(),
                emptyShell(),
                emptyShell(),
            ),
        )
    }

    private fun writeLegacyPasswordTree(
        snapshot: PasswordTreeSnapshot,
        legacyCryptoProvider: de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider,
    ) {
        Files.write(passwordTreeFile, legacyCryptoProvider.encrypt(legacyPasswordTreePayloadWriter.write(snapshot)).toByteArray())
    }

    private fun createMigrationService() = PasswordTreeKeyDerivationMigrationService(
        configuration = configuration,
        passwordTreeEnvelope = passwordTreeEnvelope,
        legacyPasswordTreePayloadReader = LegacyPasswordTreePayloadReader(configuration, systemOperation),
        passwordTreePayloadWriter = passwordTreePayloadWriter,
        systemOperation = systemOperation,
    )

    private fun createPasswordTreeReader(currentCryptoProvider: de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider) =
        PasswordTreeReader(
            systemOperation = systemOperation,
            configuration = configuration,
            cryptoProvider = currentCryptoProvider,
            passwordTreeEnvelope = passwordTreeEnvelope,
            passwordTreePayloadReader = PasswordTreePayloadReader(configuration, systemOperation),
        )
}

private fun de.pflugradts.passbird.domain.model.egg.Egg.decryptedSummary(
    cryptoProvider: de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider,
) = buildList {
    add(associatedNest().name)
    add(cryptoProvider.decrypt(viewEggId()).asString())
    add(cryptoProvider.decrypt(viewPassword()).asString())
    proteins.filter { it.isPresent }.forEach {
        add(cryptoProvider.decrypt(it.get().viewType()).asString())
        add(cryptoProvider.decrypt(it.get().viewStructure()).asString())
    }
}.joinToString(":")
