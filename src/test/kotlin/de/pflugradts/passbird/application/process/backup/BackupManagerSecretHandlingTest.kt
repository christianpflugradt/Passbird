package de.pflugradts.passbird.application.process.backup

import de.pflugradts.passbird.application.PassbirdRunContext
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.PASSWORD_TREE_FILENAME
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope
import de.pflugradts.passbird.application.security.createAesGcmCipherForTesting
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.PasswordTreeAdapterPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

class BackupManagerSecretHandlingTest {

    @field:TempDir
    lateinit var workingDirectory: Path

    private val treeBackupSettings = mockk<Configuration.BackupSettings>()
    private val configuration = mockk<Configuration>()
    private val systemOperation = SystemOperation()
    private val cryptoProvider = mockk<CryptoProvider>()
    private val passwordTreeEnvelope = PasswordTreeEnvelope()
    private val passwordTreeAdapterPort = mockk<PasswordTreeAdapterPort>(relaxed = true)

    @BeforeEach
    fun setup() {
        fakeConfiguration(
            instance = configuration,
            withKeyStoreLocation = workingDirectory.toString(),
            withPasswordTreeLocation = workingDirectory.toString(),
        )
        every { configuration.application.backup.location } returns ""
        every { configuration.application.backup.numberOfBackups } returns 0
        every { configuration.application.backup.configuration } returns mockk<Configuration.BackupSettings>(relaxed = true)
        every { configuration.application.backup.keyStore } returns mockk<Configuration.BackupSettings>(relaxed = true)
        every { configuration.application.backup.passwordTree } returns treeBackupSettings
        every { treeBackupSettings.enabled } returns true
        every { treeBackupSettings.numberOfBackups } returns 3
        every { treeBackupSettings.location } returns ""
    }

    @Test
    fun `should scramble decrypted password tree shells after comparison`() {
        val currentShell = spyk(shellOf("current"))
        val lastBackupShell = spyk(shellOf("current"))
        every { cryptoProvider.decrypt(any()) } returnsMany listOf(currentShell, lastBackupShell)
        val backupManager = BackupManager(
            configuration = configuration,
            runContext = PassbirdRunContext(workingDirectory.toString().toDirectory(), Slot.DEFAULT),
            systemOperation = systemOperation,
            cryptoProvider = cryptoProvider,
            passwordTreeEnvelope = passwordTreeEnvelope,
            passwordTreeAdapterPort = passwordTreeAdapterPort,
        )
        val passwordTreeFile = workingDirectory.resolve(PASSWORD_TREE_FILENAME)
        val encryptedTree = passwordTreeEnvelope.wrap(
            createAesGcmCipherForTesting().encrypt(shellOf("current")).toByteArray(),
        )
        Files.write(passwordTreeFile, encryptedTree)
        Files.copy(
            passwordTreeFile,
            workingDirectory.resolve("passbird_2000-01-01_00-00-00.tree"),
            REPLACE_EXISTING,
        )

        backupManager.run()

        verify(exactly = 1) { currentShell.scramble() }
        verify(exactly = 1) { lastBackupShell.scramble() }
    }
}
