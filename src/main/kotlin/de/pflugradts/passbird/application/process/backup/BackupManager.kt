package de.pflugradts.passbird.application.process.backup

import de.pflugradts.passbird.application.Directory
import de.pflugradts.passbird.application.RunContext
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.CONFIGURATION_FILENAME
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.KEYSTORE_FILENAME
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.PASSWORD_TREE_FILENAME
import de.pflugradts.passbird.application.passwordtree.PasswordTreeEnvelope
import de.pflugradts.passbird.application.process.Finalizer
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.EncryptedShell.Companion.encryptedShellOf
import de.pflugradts.passbird.domain.model.shell.Shell
import de.pflugradts.passbird.domain.model.shell.Shell.Companion.emptyShell
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import de.pflugradts.passbird.domain.service.password.tree.PasswordTreeAdapterPort
import jakarta.inject.Inject
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val backupTimestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
private const val BACKUP_TIMESTAMP_PATTERN = "\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}(?:_\\d+)?"

class BackupManager @Inject constructor(
    private val configuration: ReadableConfiguration,
    private val runContext: RunContext,
    private val systemOperation: SystemOperation,
    private val cryptoProvider: CryptoProvider,
    private val passwordTreeEnvelope: PasswordTreeEnvelope,
    private val passwordTreeAdapterPort: PasswordTreeAdapterPort,
) : Finalizer {
    private val backupConfiguration get() = configuration.application.backup
    override fun run() {
        listOf(
            Triple(backupConfiguration.configuration, runContext.homeDirectory, CONFIGURATION_FILENAME),
            Triple(backupConfiguration.passwordTree, configuration.adapter.passwordTree.location.toDirectory(), PASSWORD_TREE_FILENAME),
            Triple(backupConfiguration.keyStore, configuration.adapter.keyStore.location.toDirectory(), KEYSTORE_FILENAME),
        ).forEach { (settings, directory, fileName) ->
            if (settings.enabled && numberOfBackups(settings) > 0) {
                val current = systemOperation.resolvePath(directory, fileName.toFileName())
                ensurePasswordTreeExists(current, fileName)
                if (!systemOperation.exists(current)) return@forEach
                val limit = numberOfBackups(settings)
                val backupDirectory = systemOperation.getPath(runContext.homeDirectory)
                    .resolve(settings.location ?: backupConfiguration.location)
                    .toString().toDirectory()
                if (!systemOperation.exists(backupDirectory)) systemOperation.createDirectory(backupDirectory)
                val backups = systemOperation.getFileNames(backupDirectory).filter {
                    it.value.matches("${fileName.stem()}_$BACKUP_TIMESTAMP_PATTERN\\.${fileName.extension()}".toRegex())
                }.sortedBy { it.value }
                val backupWasCreated = if (backups.isNotEmpty()) {
                    val lastBackup = systemOperation.resolvePath(backupDirectory, backups.last())
                    if (backupContentHasChanged(current, lastBackup, fileName)) {
                        backup(directory, fileName, backupDirectory)
                        true
                    } else {
                        false
                    }
                } else {
                    backup(directory, fileName, backupDirectory)
                    true
                }
                if (backupWasCreated) {
                    backups.take(0.coerceAtLeast((backups.size + 1) - limit)).forEach {
                        systemOperation.delete(systemOperation.resolvePath(backupDirectory, it))
                    }
                }
            }
        }
    }

    private fun ensurePasswordTreeExists(current: Path, fileName: String) {
        if (fileName == PASSWORD_TREE_FILENAME && !systemOperation.exists(current)) {
            passwordTreeAdapterPort.sync(passwordTreeAdapterPort.restore())
        }
    }

    private fun numberOfBackups(settings: ReadableConfiguration.BackupSettings) =
        settings.numberOfBackups ?: configuration.application.backup.numberOfBackups

    private fun backupContentHasChanged(current: Path, lastBackup: Path, fileName: String) = if (fileName == PASSWORD_TREE_FILENAME) {
        backupContentHasChanged(current, lastBackup)
    } else {
        !systemOperation.readBytesFromFile(current).contentEquals(systemOperation.readBytesFromFile(lastBackup))
    }

    private fun backupContentHasChanged(current: Path, lastBackup: Path): Boolean {
        val currentShell = readComparablePasswordTreeShellOrNull(current)
        return try {
            val lastBackupShell = readComparablePasswordTreeShellOrNull(lastBackup)
            try {
                currentShell == null || lastBackupShell == null || currentShell != lastBackupShell
            } finally {
                lastBackupShell?.scramble()
            }
        } finally {
            currentShell?.scramble()
        }
    }

    private fun readComparablePasswordTreeShellOrNull(path: Path): Shell? = systemOperation.readBytesFromFile(path).let {
        when {
            it.isEmpty() -> emptyShell()

            passwordTreeEnvelope.isCurrent(it) -> cryptoProvider.decrypt(
                encryptedShellOf(passwordTreeEnvelope.unwrap(it)),
            )

            else -> null
        }
    }

    private fun backup(directory: Directory, fileName: String, backupDirectory: Directory) {
        val timestamp = LocalDateTime.now(systemOperation.clock).format(backupTimestampFormatter)
        val backupName = backupName(fileName, backupDirectory, timestamp)
        systemOperation.copyTo(
            systemOperation.resolvePath(directory, fileName.toFileName()),
            systemOperation.resolvePath(backupDirectory, backupName.toFileName()),
        )
    }

    private fun backupName(fileName: String, backupDirectory: Directory, timestamp: String): String {
        val backupNamePrefix = "${fileName.stem()}_$timestamp"
        val suffixes = systemOperation.getFileNames(backupDirectory).mapNotNull {
            it.value.backupSuffixFor(backupNamePrefix, fileName.extension())
        }
        val collisionSuffix = suffixes.maxOrNull()?.let { "_${(it + 1).toString().padStart(3, '0')}" }.orEmpty()
        return "$backupNamePrefix$collisionSuffix.${fileName.extension()}"
    }
}

private fun String.stem() = substring(0, indexOf("."))
private fun String.extension() = substring(indexOf(".") + 1)
private fun String.backupSuffixFor(backupNamePrefix: String, extension: String): Long? {
    val regex = "${Regex.escape(backupNamePrefix)}(?:_(\\d+))?\\.${Regex.escape(extension)}".toRegex()
    val match = regex.matchEntire(this) ?: return null
    return match.groupValues[1].takeIf { it.isNotEmpty() }?.toLong() ?: 0
}
