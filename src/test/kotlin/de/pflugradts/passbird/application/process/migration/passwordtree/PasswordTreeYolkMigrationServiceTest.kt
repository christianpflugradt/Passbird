package de.pflugradts.passbird.application.process.migration.passwordtree

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.adapter.passwordtree.PasswordTreeReader
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.passwordtree.LegacyCurrentPasswordTreePayloadReader
import de.pflugradts.passbird.application.passwordtree.LegacyCurrentPasswordTreePayloadWriter
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
import de.pflugradts.passbird.domain.service.password.tree.emptyFavorites
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
class PasswordTreeYolkMigrationServiceTest {
    private val configuration = mockk<Configuration>()
    private val systemOperation = SystemOperation()
    private val passwordTreeEnvelope = PasswordTreeEnvelope()
    private val legacyCurrentPasswordTreePayloadWriter = LegacyCurrentPasswordTreePayloadWriter()
    private val passwordTreePayloadWriter = PasswordTreePayloadWriter()
    private lateinit var passwordTreeDirectory: Path
    private lateinit var passwordTreeFile: Path

    @BeforeEach
    fun setup() {
        passwordTreeDirectory = Files.createTempDirectory("passbird-yolk-migration")
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
    fun `should migrate older current password tree content to current yolk format without losing data`() {
        val cryptoProvider = createAesGcmCipherForTesting()
        val detector = PasswordTreeYolkMigrationDetector(configuration, systemOperation)
        writeLegacyCurrentPasswordTree(createLegacyCurrentSnapshot(cryptoProvider), cryptoProvider)

        expectThat(detector.detect().required).isTrue()

        createMigrationService().migrate(createTestKeyShell())
        val restored = createPasswordTreeReader(cryptoProvider).restore()

        expectThat(detector.detect().required) isEqualTo false
        expectThat(passwordTreeEnvelope.isCurrent(Files.readAllBytes(passwordTreeFile))).isTrue()
        expectThat(restored.get().toList().map { it.decryptedSummaryWithYolkMigration(cryptoProvider) }) containsExactly listOf(
            "DEFAULT:email:Password1:user:user@example.com",
            "S3:bank:Password2",
        )
        expectThat(
            restored.memory()[Slot.DEFAULT].get()[0].map { cryptoProvider.decrypt(it).asString() }.orElse(""),
        ) isEqualTo "email"
        expectThat(
            restored.memory()[Slot.S3].get()[0].map { cryptoProvider.decrypt(it).asString() }.orElse(""),
        ) isEqualTo "bank"
        expectThat(
            restored.favorites()[Slot.DEFAULT].get()[0].map { cryptoProvider.decrypt(it).asString() }.orElse(""),
        ) isEqualTo "email"
        expectThat(restored.nests()[Slot.S1.index() - 1].asString()) isEqualTo "work"
        expectThat(restored.nests()[Slot.S3.index() - 1].asString()) isEqualTo "finance"
    }

    @Test
    fun `should scramble migration payload shells during yolk migration`() {
        val cryptoProvider = createAesGcmCipherForTesting()
        Files.write(
            passwordTreeFile,
            wrapLegacyCurrentPasswordTree(cryptoProvider.encrypt(shellOf("current payload")).toByteArray()),
        )
        val legacyCurrentPasswordTreePayloadReader = mockk<LegacyCurrentPasswordTreePayloadReader>()
        val passwordTreePayloadWriter = mockk<PasswordTreePayloadWriter>()
        val payloadShell = spyk(shellOf("migrated payload"))
        val nestShell = spyk(shellOf("finance"))
        lateinit var decryptedPayloadShell: Shell
        var decryptedPayloadBytes = emptyList<Byte>()
        every { legacyCurrentPasswordTreePayloadReader.read(any()) } answers {
            decryptedPayloadShell = firstArg()
            decryptedPayloadBytes = decryptedPayloadShell.toByteArray().toList()
            PasswordTreeSnapshot(
                favorites = emptyFavorites(),
                nests = List(Slot.CAPACITY) { if (it == 0) nestShell else emptyShell() },
            )
        }
        every { passwordTreePayloadWriter.write(any()) } returns payloadShell

        PasswordTreeYolkMigrationService(
            configuration = configuration,
            legacyCurrentPasswordTreePayloadReader = legacyCurrentPasswordTreePayloadReader,
            passwordTreePayloadWriter = passwordTreePayloadWriter,
            systemOperation = systemOperation,
        ).migrate(createTestKeyShell())

        expectThat(decryptedPayloadShell.toByteArray().toList()) isNotEqualTo decryptedPayloadBytes
        verify(exactly = 1) { payloadShell.scramble() }
        verify(exactly = 1) { nestShell.scramble() }
    }

    private fun createLegacyCurrentSnapshot(
        cryptoProvider: de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider,
    ): PasswordTreeSnapshot {
        val defaultEgg = createEgg(
            slot = Slot.DEFAULT,
            eggIdShell = cryptoProvider.encrypt(shellOf("email")),
            passwordShell = cryptoProvider.encrypt(shellOf("Password1")),
        ).apply {
            updateProtein(
                slot = Slot.S1,
                typeShell = cryptoProvider.encrypt(shellOf("user")),
                structureShell = cryptoProvider.encrypt(shellOf("user@example.com")),
            )
        }
        val nestedEgg = createEgg(
            slot = Slot.S3,
            eggIdShell = cryptoProvider.encrypt(shellOf("bank")),
            passwordShell = cryptoProvider.encrypt(shellOf("Password2")),
        )
        return PasswordTreeSnapshot(
            eggs = listOf(defaultEgg, nestedEgg),
            favorites = emptyFavorites().apply {
                this[Slot.DEFAULT].get().assign(Slot.DEFAULT, defaultEgg.viewEggId())
            },
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

    private fun writeLegacyCurrentPasswordTree(
        snapshot: PasswordTreeSnapshot,
        cryptoProvider: de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider,
    ) {
        Files.write(
            passwordTreeFile,
            wrapLegacyCurrentPasswordTree(
                cryptoProvider.encrypt(legacyCurrentPasswordTreePayloadWriter.write(snapshot)).toByteArray(),
            ),
        )
    }

    private fun createMigrationService() = PasswordTreeYolkMigrationService(
        configuration = configuration,
        legacyCurrentPasswordTreePayloadReader = LegacyCurrentPasswordTreePayloadReader(configuration, systemOperation),
        passwordTreePayloadWriter = passwordTreePayloadWriter,
        systemOperation = systemOperation,
    )

    private fun createPasswordTreeReader(cryptoProvider: de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider) =
        PasswordTreeReader(
            systemOperation = systemOperation,
            configuration = configuration,
            cryptoProvider = cryptoProvider,
            passwordTreeEnvelope = passwordTreeEnvelope,
            passwordTreePayloadReader = PasswordTreePayloadReader(configuration, systemOperation),
        )
}

private fun de.pflugradts.passbird.domain.model.egg.Egg.decryptedSummaryWithYolkMigration(
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
