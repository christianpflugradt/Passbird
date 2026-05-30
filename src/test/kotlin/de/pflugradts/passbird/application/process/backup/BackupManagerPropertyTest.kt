package de.pflugradts.passbird.application.process.backup

import de.pflugradts.passbird.PROPERTY
import de.pflugradts.passbird.application.PassbirdRunContext
import de.pflugradts.passbird.application.configuration.Configuration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.PASSWORD_TREE_FILENAME
import de.pflugradts.passbird.application.configuration.fakeConfiguration
import de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope
import de.pflugradts.passbird.application.security.createAesGcmCipherForTesting
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.EncryptedShell.Companion.encryptedShellOf
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.shellOf
import de.pflugradts.passbird.domain.model.slot.Slot
import de.pflugradts.passbird.domain.service.password.tree.PasswordTreeAdapterPort
import de.pflugradts.passbird.property.textValues
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Tag(PROPERTY)
class BackupManagerPropertyTest {

    @Test
    fun keepsOnlyTheLatestDistinctPasswordTreeBackupsWithinTheRetentionLimit() {
        runBlocking {
            checkAll(20, Arb.list(textValues(), 1..8), Arb.int(1..4)) { backupContents, backupLimit ->
                val workingDirectory = Files.createTempDirectory("passbird-backup-property")

                try {
                    val configuration = mockk<Configuration>()
                    val passwordTreeBackupSettings = mockk<Configuration.BackupSettings>()
                    val systemOperation = spyk(SystemOperation())
                    val cryptoProvider = createAesGcmCipherForTesting()
                    val passwordTreeEnvelope = PasswordTreeEnvelope()
                    val now = Instant.parse("2026-01-01T00:00:00Z")
                    every { systemOperation.clock } answers { Clock.fixed(now, ZoneOffset.UTC) }
                    val backupManager = BackupManager(
                        configuration = configuration,
                        runContext = PassbirdRunContext(workingDirectory.toString().toDirectory(), Slot.DEFAULT),
                        systemOperation = systemOperation,
                        cryptoProvider = cryptoProvider,
                        passwordTreeEnvelope = passwordTreeEnvelope,
                        passwordTreeAdapterPort = mockk<PasswordTreeAdapterPort>(relaxed = true),
                    )

                    fakeConfiguration(
                        instance = configuration,
                        withKeyStoreLocation = workingDirectory.toString(),
                        withPasswordTreeLocation = workingDirectory.toString(),
                    )
                    every { configuration.application.backup.location } returns ""
                    every { configuration.application.backup.numberOfBackups } returns backupLimit
                    every { configuration.application.backup.configuration } returns mockk<Configuration.BackupSettings>(relaxed = true)
                    every { configuration.application.backup.keyStore } returns mockk<Configuration.BackupSettings>(relaxed = true)
                    every { configuration.application.backup.passwordTree } returns passwordTreeBackupSettings
                    every { passwordTreeBackupSettings.enabled } returns true
                    every { passwordTreeBackupSettings.location } returns ""
                    every { passwordTreeBackupSettings.numberOfBackups } returns backupLimit

                    backupContents.forEach { content ->
                        Files.write(
                            workingDirectory.resolve(PASSWORD_TREE_FILENAME),
                            passwordTreeEnvelope.wrap(cryptoProvider.encrypt(shellOf(content)).toByteArray()),
                        )
                        backupManager.run()
                    }

                    val actual = Files.list(workingDirectory)
                        .filter { path ->
                            path.fileName.toString().startsWith(PASSWORD_TREE_FILENAME.substringBefore('.')) &&
                                path.fileName.toString() != PASSWORD_TREE_FILENAME
                        }
                        .sorted()
                        .toList()
                        .map { path ->
                            cryptoProvider.decrypt(encryptedShellOf(passwordTreeEnvelope.unwrap(Files.readAllBytes(path)))).asString()
                        }

                    expectThat(actual) isEqualTo expectedBackups(backupContents, backupLimit)
                } finally {
                    workingDirectory.toFile().deleteRecursively()
                }
            }
        }
    }
}

private fun expectedBackups(backupContents: List<String>, backupLimit: Int) = backupContents
    .fold(mutableListOf<String>()) { distinctBackups, currentContent ->
        if (distinctBackups.lastOrNull() != currentContent) {
            distinctBackups.add(currentContent)
        }
        distinctBackups
    }.takeLast(backupLimit)
