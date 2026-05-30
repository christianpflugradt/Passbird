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
import de.pflugradts.passbird.application.security.createTestKeyShell
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.password.tree.emptyMemory
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

@Tag(INTEGRATION)
class PasswordTreeFavoritesMigrationServiceTest {
    private val configuration = mockk<Configuration>()
    private val systemOperation = SystemOperation()
    private val cryptoProvider = createAesGcmCipherForTesting()
    private val passwordTreeEnvelope = PasswordTreeEnvelope()
    private val legacyPasswordTreePayloadWriter = LegacyPasswordTreePayloadWriter()
    private lateinit var passwordTreeDirectory: Path
    private lateinit var passwordTreeFile: Path

    @BeforeEach
    fun setup() {
        passwordTreeDirectory = Files.createTempDirectory("passbird-favorites-migration")
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
    fun `should migrate current password tree format to favorites-capable format without losing state`() {
        val detector = PasswordTreeFavoritesMigrationDetector(configuration, passwordTreeEnvelope, systemOperation)
        writeLegacyCurrentPasswordTree(createLegacySnapshot())

        expectThat(detector.detect().required).isTrue()

        createMigrationService().migrate(createTestKeyShell())
        val restored = PasswordTreeReader(
            systemOperation = systemOperation,
            configuration = configuration,
            cryptoProvider = cryptoProvider,
            passwordTreeEnvelope = passwordTreeEnvelope,
            passwordTreePayloadReader = PasswordTreePayloadReader(configuration, systemOperation),
        ).restore()

        expectThat(detector.detect().required) isEqualTo false
        expectThat(passwordTreeEnvelope.isCurrent(Files.readAllBytes(passwordTreeFile))).isTrue()
        expectThat(restored.get().toList().map { it.decryptedSummary(cryptoProvider) }) containsExactly listOf(
            "DEFAULT:email:Password1",
            "S3:bank:Password2",
        )
        expectThat(restored.memory()[Slot.DEFAULT].get()[0].map { cryptoProvider.decrypt(it).asString() }.orElse("")) isEqualTo "email"
        expectThat(restored.favorites()[Slot.DEFAULT].get().any { it.isPresent }).isEqualTo(false)
        expectThat(restored.nests()[Slot.S1.index() - 1].asString()) isEqualTo "work"
        expectThat(restored.nests()[Slot.S3.index() - 1].asString()) isEqualTo "finance"
    }

    @Test
    fun `should scramble decrypted and written payload shells during migration`() {
        writeLegacyCurrentPasswordTree(PasswordTreeSnapshot())
        val legacyPasswordTreePayloadReader = mockk<LegacyPasswordTreePayloadReader>()
        val passwordTreePayloadWriter = mockk<PasswordTreePayloadWriter>()
        val payloadShell = spyk(shellOf("current payload"))
        lateinit var decryptedPayloadShell: Shell
        var decryptedPayloadBytes = emptyList<Byte>()
        every { legacyPasswordTreePayloadReader.read(any()) } answers {
            decryptedPayloadShell = firstArg()
            decryptedPayloadBytes = decryptedPayloadShell.toByteArray().toList()
            PasswordTreeSnapshot()
        }
        every { passwordTreePayloadWriter.write(any()) } returns payloadShell

        PasswordTreeFavoritesMigrationService(
            configuration = configuration,
            legacyPasswordTreePayloadReader = legacyPasswordTreePayloadReader,
            passwordTreeEnvelope = passwordTreeEnvelope,
            passwordTreePayloadWriter = passwordTreePayloadWriter,
            systemOperation = systemOperation,
        ).migrate(createTestKeyShell())

        expectThat(decryptedPayloadShell.toByteArray().toList()) isNotEqualTo decryptedPayloadBytes
        verify(exactly = 1) { payloadShell.scramble() }
    }

    private fun createLegacySnapshot(): PasswordTreeSnapshot {
        val defaultEgg = createEgg(
            slot = Slot.DEFAULT,
            eggIdShell = cryptoProvider.encrypt(shellOf("email")),
            passwordShell = cryptoProvider.encrypt(shellOf("Password1")),
        )
        val nestedEgg = createEgg(
            slot = Slot.S3,
            eggIdShell = cryptoProvider.encrypt(shellOf("bank")),
            passwordShell = cryptoProvider.encrypt(shellOf("Password2")),
        )
        return PasswordTreeSnapshot(
            eggs = listOf(defaultEgg, nestedEgg),
            memory = emptyMemory().apply {
                this[Slot.DEFAULT].get().memorize(defaultEgg.viewEggId(), null)
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

    private fun writeLegacyCurrentPasswordTree(snapshot: PasswordTreeSnapshot) {
        Files.write(
            passwordTreeFile,
            passwordTreeEnvelope.wrapLegacyCurrent(
                cryptoProvider.encrypt(legacyPasswordTreePayloadWriter.write(snapshot)).toByteArray(),
            ),
        )
    }

    private fun createMigrationService() = PasswordTreeFavoritesMigrationService(
        configuration = configuration,
        legacyPasswordTreePayloadReader = LegacyPasswordTreePayloadReader(configuration, systemOperation),
        passwordTreeEnvelope = passwordTreeEnvelope,
        passwordTreePayloadWriter = PasswordTreePayloadWriter(),
        systemOperation = systemOperation,
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
