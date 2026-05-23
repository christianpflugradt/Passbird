package de.pflugradts.passbird.application.process.backup

import de.pflugradts.passbird.application.Directory
import de.pflugradts.passbird.application.RunContext
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.CONFIGURATION_FILENAME
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.KEYSTORE_FILENAME
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.PASSWORD_TREE_FILENAME
import de.pflugradts.passbird.application.process.Finalizer
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import de.pflugradts.passbird.domain.model.shell.EncryptedShell.Companion.encryptedShellOf
import de.pflugradts.passbird.domain.service.password.encryption.CryptoProvider
import jakarta.inject.Inject
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BackupManager @Inject constructor(
    private val configuration: ReadableConfiguration,
    private val runContext: RunContext,
    private val systemOperation: SystemOperation,
    private val cryptoProvider: CryptoProvider,
) : Finalizer {
    private val backupConfiguration get() = configuration.application.backup
    override fun run() {
        listOf(
            Triple(backupConfiguration.configuration, runContext.homeDirectory, CONFIGURATION_FILENAME),
            Triple(backupConfiguration.passwordTree, configuration.adapter.passwordTree.location.toDirectory(), PASSWORD_TREE_FILENAME),
            Triple(backupConfiguration.keyStore, configuration.adapter.keyStore.location.toDirectory(), KEYSTORE_FILENAME),
        ).forEach { (settings, directory, fileName) ->
            if (settings.enabled && numberOfBackups(settings) > 0) {
                val backupDirectory = systemOperation.getPath(runContext.homeDirectory)
                    .resolve(settings.location ?: backupConfiguration.location)
                    .toString().toDirectory()
                if (!systemOperation.exists(backupDirectory)) systemOperation.createDirectory(backupDirectory)
                val backupPattern = "\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}"
                val backups = systemOperation.getFileNames(backupDirectory).filter {
                    it.value.matches("${fileName.stem()}_$backupPattern\\.${fileName.extension()}".toRegex())
                }.sortedBy { it.value }
                if (backups.isNotEmpty()) {
                    val current = systemOperation.resolvePath(directory, fileName.toFileName())
                    val lastBackup = systemOperation.resolvePath(backupDirectory, backups.last())
                    if (backupContentHasChanged(current, lastBackup, fileName)) {
                        backup(directory, fileName, backupDirectory)
                    }
                    backups.take(0.coerceAtLeast((backups.size + 1) - numberOfBackups(settings))).forEach {
                        systemOperation.delete(systemOperation.resolvePath(backupDirectory, it))
                    }
                } else {
                    backup(directory, fileName, backupDirectory)
                }
            }
        }
    }

    private fun numberOfBackups(settings: ReadableConfiguration.BackupSettings) =
        settings.numberOfBackups ?: configuration.application.backup.numberOfBackups

    private fun backupContentHasChanged(current: Path, lastBackup: Path, fileName: String) =
        !readComparableBytes(current, fileName).contentEquals(readComparableBytes(lastBackup, fileName))

    private fun readComparableBytes(path: Path, fileName: String) = if (fileName == PASSWORD_TREE_FILENAME) {
        readPasswordTreeBytes(path)
    } else {
        systemOperation.readBytesFromFile(path)
    }

    private fun readPasswordTreeBytes(path: Path) = systemOperation.readBytesFromFile(path).let {
        if (it.isEmpty()) byteArrayOf() else cryptoProvider.decrypt(encryptedShellOf(it)).toByteArray()
    }

    private fun backup(directory: Directory, fileName: String, backupDirectory: Directory) {
        val format = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
        val backupName = "${fileName.stem()}_${LocalDateTime.now().format(format)}.${fileName.extension()}"
        systemOperation.copyTo(
            systemOperation.resolvePath(directory, fileName.toFileName()),
            systemOperation.resolvePath(backupDirectory, backupName.toFileName()),
        )
    }
}

private fun String.stem() = substring(0, indexOf("."))
private fun String.extension() = substring(indexOf(".") + 1)
