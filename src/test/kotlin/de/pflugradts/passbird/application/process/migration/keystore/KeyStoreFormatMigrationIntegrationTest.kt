package de.pflugradts.passbird.application.process.migration.keystore

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.adapter.keystore.JceksKeyStoreService
import de.pflugradts.passbird.adapter.keystore.KeyStoreService
import de.pflugradts.passbird.adapter.keystore.MigrationKeyStoreService
import de.pflugradts.passbird.adapter.passwordtree.PasswordTreeReader
import de.pflugradts.passbird.application.UserInterfaceAdapterPort
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.fakeUserInterfaceAdapterPort
import de.pflugradts.passbird.application.keystore.KeyStoreFormat
import de.pflugradts.passbird.application.keystore.KeyStoreFormatDetector
import de.pflugradts.passbird.application.passwordtree.LegacyPasswordTreePayloadReader
import de.pflugradts.passbird.application.passwordtree.LegacyPasswordTreePayloadWriter
import de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadReader
import de.pflugradts.passbird.application.passwordtree.PasswordTreePayloadWriter
import de.pflugradts.passbird.application.passwordtree.PasswordTreeSnapshot
import de.pflugradts.passbird.application.process.migration.MigrationAuthenticationService
import de.pflugradts.passbird.application.process.migration.MigrationRunner
import de.pflugradts.passbird.application.process.migration.passwordtree.PasswordTreeKeyDerivationMigration
import de.pflugradts.passbird.application.process.migration.passwordtree.PasswordTreeKeyDerivationMigrationDetector
import de.pflugradts.passbird.application.process.migration.passwordtree.PasswordTreeKeyDerivationMigrationService
import de.pflugradts.passbird.application.security.createAesGcmCipherForTesting
import de.pflugradts.passbird.application.security.createLegacyAesGcmCipherForTesting
import de.pflugradts.passbird.application.security.createTestKeyShell
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.egg.Egg.Companion.createEgg
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.model.transfer.Input.Companion.inputOf
import de.pflugradts.passbird.domain.service.nest.createNestServiceForTesting
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.emptyMemory
import io.mockk.mockk
import io.mockk.verify
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
class KeyStoreFormatMigrationIntegrationTest {
    private val configuration = mockk<Configuration>()
    private val userInterfaceAdapterPort = mockk<UserInterfaceAdapterPort>()
    private val systemOperation = SystemOperation()
    private val keyStoreFormatDetector = KeyStoreFormatDetector()
    private val passwordTreeEnvelope = PasswordTreeEnvelope()
    private val legacyPasswordTreePayloadWriter = LegacyPasswordTreePayloadWriter()
    private val passwordTreePayloadWriter = PasswordTreePayloadWriter()
    private lateinit var homeDirectory: Path
    private lateinit var keyStoreFile: Path
    private lateinit var passwordTreeFile: Path

    @BeforeEach
    fun setup() {
        homeDirectory = Files.createTempDirectory("passbird-keystore-migration")
        keyStoreFile = homeDirectory.resolve(ReadableConfiguration.KEYSTORE_FILENAME)
        passwordTreeFile = homeDirectory.resolve(ReadableConfiguration.PASSWORD_TREE_FILENAME)
        fakeConfiguration(
            instance = configuration,
            withKeyStoreLocation = homeDirectory.toString(),
            withPasswordTreeLocation = homeDirectory.toString(),
            withEggIdMemoryEnabled = true,
            withEggIdMemoryPersisted = true,
        )
    }

    @AfterEach
    fun cleanup() {
        expectThat(File(homeDirectory.toString()).deleteRecursively()).isTrue()
    }

    @Test
    fun `should migrate legacy keystore and legacy password tree in one authenticated migration session`() {
        val password = "p4s5wrD!"
        val migrationFixture = createMigrationFixture(password)
        fakeUserInterfaceAdapterPort(
            instance = userInterfaceAdapterPort,
            withTheseSecureInputs = listOf(inputOf(shellOf(password))),
        )
        writeLegacyState(password)
        assertMigrationDetected(migrationFixture)
        migrationFixture.migrationRunner.run(
            migrationFixture.keyStoreMigrationDetector.detect() + migrationFixture.passwordTreeMigrationDetector.detect(),
        )
        migrationFixture.migrationAuthenticationService.invalidate()
        assertMigratedState(password, migrationFixture)
        verify(exactly = 1) { userInterfaceAdapterPort.receiveSecurely(any()) }
    }

    private fun createMigrationFixture(password: String): MigrationFixture {
        val migrationKeyStoreService = MigrationKeyStoreService(
            currentKeyStoreService = KeyStoreService(systemOperation),
            jceksKeyStoreService = JceksKeyStoreService(systemOperation),
            keyStoreFormatDetector = keyStoreFormatDetector,
            systemOperation = systemOperation,
        )
        val migrationAuthenticationService = MigrationAuthenticationService(
            configuration = configuration,
            keyStoreAdapterPort = migrationKeyStoreService,
            userInterfaceAdapterPort = userInterfaceAdapterPort,
            systemOperation = systemOperation,
        )
        val keyStoreMigrationDetector = KeyStoreFormatMigrationDetector(configuration, keyStoreFormatDetector, systemOperation)
        val passwordTreeMigrationDetector =
            PasswordTreeKeyDerivationMigrationDetector(configuration, passwordTreeEnvelope, systemOperation)
        val keyStoreMigration = KeyStoreFormatMigration(
            keyStoreFormatMigrationService = KeyStoreFormatMigrationService(
                configuration,
                migrationKeyStoreService,
                systemOperation,
            ),
            migrationAuthenticationService = migrationAuthenticationService,
            systemOperation = systemOperation,
        )
        val passwordTreeMigration = PasswordTreeKeyDerivationMigration(
            migrationAuthenticationService = migrationAuthenticationService,
            passwordTreeKeyDerivationMigrationService = PasswordTreeKeyDerivationMigrationService(
                configuration = configuration,
                passwordTreeEnvelope = passwordTreeEnvelope,
                legacyPasswordTreePayloadReader = LegacyPasswordTreePayloadReader(configuration, systemOperation),
                passwordTreePayloadWriter = passwordTreePayloadWriter,
                systemOperation = systemOperation,
            ),
            systemOperation = systemOperation,
        )
        return MigrationFixture(
            migrationRunner = MigrationRunner(setOf(passwordTreeMigration, keyStoreMigration)),
            migrationAuthenticationService = migrationAuthenticationService,
            keyStoreMigrationDetector = keyStoreMigrationDetector,
            passwordTreeMigrationDetector = passwordTreeMigrationDetector,
        )
    }

    private fun writeLegacyState(password: String) {
        writeLegacyKeyStore(password)
        val legacyCryptoProvider = createLegacyAesGcmCipherForTesting()
        writeLegacyPasswordTree(createLegacySnapshot(legacyCryptoProvider), legacyCryptoProvider)
    }

    private fun assertMigrationDetected(migrationFixture: MigrationFixture) {
        expectThat(migrationFixture.keyStoreMigrationDetector.detect().required).isTrue()
        expectThat(migrationFixture.passwordTreeMigrationDetector.detect().required).isTrue()
    }

    private fun assertMigratedState(password: String, migrationFixture: MigrationFixture) {
        val currentCryptoProvider = createAesGcmCipherForTesting()
        val restoredNestService = createNestServiceForTesting()
        val restoredTree = PasswordTreeReader(
            systemOperation = systemOperation,
            configuration = configuration,
            nestService = restoredNestService,
            cryptoProvider = currentCryptoProvider,
            passwordTreeEnvelope = passwordTreeEnvelope,
            passwordTreePayloadReader = PasswordTreePayloadReader(configuration, systemOperation),
        ).restore()

        expectThat(keyStoreFormatDetector.detect(Files.readAllBytes(keyStoreFile))) isEqualTo KeyStoreFormat.PKCS12
        expectThat(KeyStoreService(systemOperation).loadKey(createPlainShell(password), keyStoreFile).success).isTrue()
        expectThat(migrationFixture.keyStoreMigrationDetector.detect().required) isEqualTo false
        expectThat(migrationFixture.passwordTreeMigrationDetector.detect().required) isEqualTo false
        expectThat(passwordTreeEnvelope.isCurrent(Files.readAllBytes(passwordTreeFile))).isTrue()
        expectThat(restoredTree.get().toList().map { it.decryptedSummary(currentCryptoProvider) }) containsExactly listOf(
            "DEFAULT:email:Password1:user:user@example.com",
            "S3:bank:Password2",
        )
        expectThat(restoredTree.memory()[Slot.DEFAULT].get()[0].map { currentCryptoProvider.decrypt(it).asString() }.orElse("")) isEqualTo
            "email"
        expectThat(restoredTree.memory()[Slot.S3].get()[0].map { currentCryptoProvider.decrypt(it).asString() }.orElse("")) isEqualTo
            "bank"
        expectThat(restoredNestService.atNestSlot(Slot.S1).get().viewNestId().asString()) isEqualTo "work"
        expectThat(restoredNestService.atNestSlot(Slot.S3).get().viewNestId().asString()) isEqualTo "finance"
    }

    private fun writeLegacyKeyStore(password: String) {
        JceksKeyStoreService(systemOperation).storeExistingKey(
            createTestKeyShell(),
            createPlainShell(password),
            keyStoreFile,
        )
    }

    private fun writeLegacyPasswordTree(snapshot: PasswordTreeSnapshot, legacyCryptoProvider: CryptoProvider) {
        Files.write(passwordTreeFile, legacyCryptoProvider.encrypt(legacyPasswordTreePayloadWriter.write(snapshot)).toByteArray())
    }

    private fun createLegacySnapshot(legacyCryptoProvider: CryptoProvider): PasswordTreeSnapshot {
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

    private fun createPlainShell(value: String) = de.pflugradts.passbird.domain.model.shell.PlainShell.plainShellOf(value.toCharArray())

    private data class MigrationFixture(
        val migrationRunner: MigrationRunner,
        val migrationAuthenticationService: MigrationAuthenticationService,
        val keyStoreMigrationDetector: KeyStoreFormatMigrationDetector,
        val passwordTreeMigrationDetector: PasswordTreeKeyDerivationMigrationDetector,
    )
}

private fun de.pflugradts.passbird.domain.model.egg.Egg.decryptedSummary(cryptoProvider: CryptoProvider) = buildList {
    add(associatedNest().name)
    add(cryptoProvider.decrypt(viewEggId()).asString())
    add(cryptoProvider.decrypt(viewPassword()).asString())
    proteins.filter { it.isPresent }.forEach {
        add(cryptoProvider.decrypt(it.get().viewType()).asString())
        add(cryptoProvider.decrypt(it.get().viewStructure()).asString())
    }
}.joinToString(":")
