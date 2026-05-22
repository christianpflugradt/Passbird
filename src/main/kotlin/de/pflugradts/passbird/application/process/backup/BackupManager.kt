package de.pflugradts.passbird.application.process.backup

import com.google.inject.Inject
import de.pflugradts.passbird.application.Directory
import de.pflugradts.passbird.application.Global
import de.pflugradts.passbird.application.configuration.ReadableConfiguration
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.CONFIGURATION_FILENAME
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.KEYSTORE_FILENAME
import de.pflugradts.passbird.application.configuration.ReadableConfiguration.Companion.PASSWORD_TREE_FILENAME
import de.pflugradts.passbird.application.process.Finalizer
import de.pflugradts.passbird.application.toDirectory
import de.pflugradts.passbird.application.toFileName
import de.pflugradts.passbird.application.util.SystemOperation
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BackupManager @Inject constructor(
    private val configuration: ReadableConfiguration,
    private val systemOperation: SystemOperation,
) : Finalizer {
    private val backupConfiguration get() = configuration.application.backup
    override fun run() {
        listOf(
            Triple(backupConfiguration.configuration, Global.homeDirectory, CONFIGURATION_FILENAME),
            Triple(backupConfiguration.passwordTree, configuration.adapter.passwordTree.location.toDirectory(), PASSWORD_TREE_FILENAME),
            Triple(backupConfiguration.keyStore, configuration.adapter.keyStore.location.toDirectory(), KEYSTORE_FILENAME),
        ).forEach { (settings, directory, fileName) ->
            if (settings.enabled && numberOfBackups(settings) > 0) {
                val backupDirectory = systemOperation.getPath(Global.homeDirectory)
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
                    if (systemOperation.readBytesFromFile(current) != systemOperation.readBytesFromFile(lastBackup)) {
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
