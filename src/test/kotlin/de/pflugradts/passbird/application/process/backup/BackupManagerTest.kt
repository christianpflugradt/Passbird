package de.pflugradts.passbird.application.process.backup

import de.pflugradts.passbird.INTEGRATION
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.PASSWORD_TREE_FILENAME
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.mainMocked
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.util.SystemOperation
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
import java.util.UUID

@Tag(INTEGRATION)
class BackupManagerTest {

    private val tempWorkingDirectory = UUID.randomUUID().toString()
    private val treeBackupSettings = mockk<Configuration.BackupSettings>()
    private val configuration = mockk<Configuration>()
    private val systemOperation = SystemOperation()
    private val backupManager = BackupManager(configuration, systemOperation)

    @BeforeEach
    fun setup() {
        expectThat(File(tempWorkingDirectory).mkdir()).isTrue()
        mainMocked(arrayOf(tempWorkingDirectory))
        fakeConfiguration(
            instance = configuration,
            withKeyStoreLocation = tempWorkingDirectory,
            withPasswordTreeLocation = tempWorkingDirectory,
        )
        every { configuration.application.backup.location } returns ""
        every { configuration.application.backup.numberOfBackups } returns 0
        every { configuration.application.backup.configuration } returns mockk<Configuration.BackupSettings>(relaxed = true)
        every { configuration.application.backup.keyStore } returns mockk<Configuration.BackupSettings>(relaxed = true)
        every { configuration.application.backup.passwordTree } returns treeBackupSettings
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
        expectThat(files()).isEmpty()
    }

    @Test
    fun `should not backup anything if backup is not enabled`() {
        // given
        every { treeBackupSettings.enabled } returns false
        every { treeBackupSettings.numberOfBackups } returns 3

        // when
        backupManager.run()

        // then
        expectThat(files()).isEmpty()
    }

    @Test
    fun `should create a backup if none exists`() {
        // given
        every { treeBackupSettings.enabled } returns true
        every { treeBackupSettings.numberOfBackups } returns 3

        // when
        backupManager.run()

        // then
        expectThat(files()) hasSize 1
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
        expectThat(files()) hasSize 2
    }

    @Test
    fun `should not create another backup if file has not changed`() {
        // given
        every { treeBackupSettings.enabled } returns true
        every { treeBackupSettings.numberOfBackups } returns 3
        updatePasswordTreeFileContent("initial")
        backupManager.run()

        // when
        backupManager.run()

        // then
        expectThat(files()) hasSize 1
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
        expectThat(files()) hasSize 3
        wait1Sec()
        updatePasswordTreeFileContent(expectedContent)
        every { treeBackupSettings.numberOfBackups } returns 1

        // when
        backupManager.run()

        // then
        expectThat(files()) hasSize 1
        val backupFile = Paths.get("$tempWorkingDirectory/${files()[0]}")
        expectThat(Files.readString(backupFile)) isEqualTo expectedContent
    }

    private fun updatePasswordTreeFileContent(content: String) =
        Files.writeString(Paths.get("$tempWorkingDirectory/$PASSWORD_TREE_FILENAME"), content)
    private fun wait1Sec() = Thread.sleep(1000)
    private fun files() = systemOperation.getFileNames(tempWorkingDirectory.toDirectory()).map { it.value }
        .filterNot { it == tempWorkingDirectory }
        .filterNot { it == PASSWORD_TREE_FILENAME }
}
