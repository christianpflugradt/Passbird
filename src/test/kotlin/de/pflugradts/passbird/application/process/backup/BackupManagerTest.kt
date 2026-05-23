package de.pflugradts.passbird.application.process.backup

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.PassbirdRunContext
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.CONFIGURATION_FILENAME
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.PASSWORD_TREE_FILENAME
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.security.createAesGcmCipherForTesting
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.application.util.posixPermissionsIfSupported
import de.pflugradts.passbird.domain.model.shell.EncryptedShell.Companion.encryptedShellOf
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE
import java.nio.file.attribute.PosixFilePermission.OWNER_READ
import java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
import java.util.UUID

@Tag(INTEGRATION)
class BackupManagerTest {

    private val tempWorkingDirectory = UUID.randomUUID().toString()
    private val runContext = PassbirdRunContext(tempWorkingDirectory.toDirectory(), Slot.DEFAULT)
    private val configurationBackupSettings = mockk<Configuration.BackupSettings>()
    private val treeBackupSettings = mockk<Configuration.BackupSettings>()
    private val configuration = mockk<Configuration>()
    private val systemOperation = SystemOperation()
    private val cryptoProvider: CryptoProvider = createAesGcmCipherForTesting()
    private val backupManager = BackupManager(configuration, runContext, systemOperation, cryptoProvider)

    @BeforeEach
    fun setup() {
        expectThat(File(tempWorkingDirectory).mkdir()).isTrue()
        fakeConfiguration(
            instance = configuration,
            withKeyStoreLocation = tempWorkingDirectory,
            withPasswordTreeLocation = tempWorkingDirectory,
        )
        every { configuration.application.backup.location } returns ""
        every { configuration.application.backup.numberOfBackups } returns 0
        every { configuration.application.backup.configuration } returns configurationBackupSettings
        every { configuration.application.backup.keyStore } returns mockk<Configuration.BackupSettings>(relaxed = true)
        every { configuration.application.backup.passwordTree } returns treeBackupSettings
        every { configurationBackupSettings.enabled } returns false
        every { configurationBackupSettings.numberOfBackups } returns 0
        every { configurationBackupSettings.location } returns ""
        every { treeBackupSettings.location } returns ""
        updatePasswordTreeFileContent("initial")
    }

    @AfterEach
    fun cleanup() {
        expectThat(File(tempWorkingDirectory).deleteRecursively()).isTrue()
    }

    @Test
    fun `should not backup anything if number of backups is 0`() {
        // given
        every { treeBackupSettings.enabled } returns true
        every { treeBackupSettings.numberOfBackups } returns 0

        // when
        backupManager.run()

        // then
        expectThat(backupFiles(PASSWORD_TREE_FILENAME)).isEmpty()
    }

    @Test
    fun `should not backup anything if backup is not enabled`() {
        // given
        every { treeBackupSettings.enabled } returns false
        every { treeBackupSettings.numberOfBackups } returns 3

        // when
        backupManager.run()

        // then
        expectThat(backupFiles(PASSWORD_TREE_FILENAME)).isEmpty()
    }

    @Test
    fun `should create a backup if none exists`() {
        // given
        every { treeBackupSettings.enabled } returns true
        every { treeBackupSettings.numberOfBackups } returns 3

        // when
        backupManager.run()

        // then
        expectThat(backupFiles(PASSWORD_TREE_FILENAME)) hasSize 1
        val backupFile = Paths.get("$tempWorkingDirectory/${backupFiles(PASSWORD_TREE_FILENAME).single()}")
        posixPermissionsIfSupported(backupFile)?.let {
            expectThat(it) isEqualTo setOf(OWNER_READ, OWNER_WRITE)
        }
    }

    @Test
    fun `should harden newly created backup directory and file permissions`() {
        // given
        every { treeBackupSettings.enabled } returns true
        every { treeBackupSettings.numberOfBackups } returns 3
        every { treeBackupSettings.location } returns null
        every { configuration.application.backup.location } returns "backups"
        val backupDirectory = Paths.get(tempWorkingDirectory).resolve("backups")

        // when
        backupManager.run()

        // then
        expectThat(Files.isDirectory(backupDirectory)).isTrue()
        posixPermissionsIfSupported(backupDirectory)?.let {
            expectThat(it) isEqualTo setOf(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE)
        }
        val backupFile = backupDirectory.resolve(backupFiles(PASSWORD_TREE_FILENAME, backupDirectory.toString()).single())
        posixPermissionsIfSupported(backupFile)?.let {
            expectThat(it) isEqualTo setOf(OWNER_READ, OWNER_WRITE)
        }
    }

    @Test
    fun `should create another backup if file has changed`() {
        // given
        every { treeBackupSettings.enabled } returns true
        every { treeBackupSettings.numberOfBackups } returns 3
        updatePasswordTreeFileContent("initial")
        backupManager.run()
        wait1Sec()
        updatePasswordTreeFileContent("updated")

        // when
        backupManager.run()

        // then
        expectThat(backupFiles(PASSWORD_TREE_FILENAME)) hasSize 2
    }

    @Test
    fun `should not create another backup if decrypted password tree content has not changed`() {
        // given
        every { treeBackupSettings.enabled } returns true
        every { treeBackupSettings.numberOfBackups } returns 3
        updatePasswordTreeFileContent("initial", cryptoProvider)
        backupManager.run()
        wait1Sec()
        updatePasswordTreeFileContent("initial", cryptoProvider)

        // when
        backupManager.run()

        // then
        expectThat(backupFiles(PASSWORD_TREE_FILENAME)) hasSize 1
    }

    @Test
    fun `should not create another backup if configuration file has not changed`() {
        // given
        every { treeBackupSettings.enabled } returns false
        every { configurationBackupSettings.enabled } returns true
        every { configurationBackupSettings.numberOfBackups } returns 3
        Files.writeString(Paths.get("$tempWorkingDirectory/$CONFIGURATION_FILENAME"), "configuration")
        backupManager.run()

        // when
        backupManager.run()

        // then
        expectThat(backupFiles(CONFIGURATION_FILENAME)) hasSize 1
    }

    @Test
    fun `should remove old backups`() {
        // given
        val expectedContent = "latest"

        every { treeBackupSettings.enabled } returns true
        every { treeBackupSettings.numberOfBackups } returns 3

        updatePasswordTreeFileContent("first")
        backupManager.run()
        wait1Sec()
        updatePasswordTreeFileContent("second")
        backupManager.run()
        wait1Sec()
        updatePasswordTreeFileContent("third")
        backupManager.run()
        expectThat(backupFiles(PASSWORD_TREE_FILENAME)) hasSize 3
        wait1Sec()
        updatePasswordTreeFileContent(expectedContent)
        every { treeBackupSettings.numberOfBackups } returns 1

        // when
        backupManager.run()

        // then
        expectThat(backupFiles(PASSWORD_TREE_FILENAME)) hasSize 1
        val backupFile = Paths.get("$tempWorkingDirectory/${backupFiles(PASSWORD_TREE_FILENAME)[0]}")
        expectThat(
            cryptoProvider.decrypt(encryptedShellOf(Files.readAllBytes(backupFile))).asString(),
        ) isEqualTo expectedContent
    }

    private fun updatePasswordTreeFileContent(content: String, cryptoProvider: CryptoProvider = this.cryptoProvider) = Files.write(
        Paths.get("$tempWorkingDirectory/$PASSWORD_TREE_FILENAME"),
        cryptoProvider.encrypt(shellOf(content)).toByteArray(),
    )
    private fun wait1Sec() = Thread.sleep(1000)
    private fun backupFiles(fileName: String, directory: String = tempWorkingDirectory) =
        systemOperation.getFileNames(directory.toDirectory())
            .map { it.value }
            .filter {
                it.matches(
                    "${fileName.substringBefore(".")}_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.${fileName.substringAfter(".")}"
                        .toRegex(),
                )
            }
}
